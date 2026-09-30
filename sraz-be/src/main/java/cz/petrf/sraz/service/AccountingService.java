package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.db.repo.*;
import cz.petrf.sraz.exception.DomainException;
import cz.petrf.sraz.exception.NotFoundException;
import cz.petrf.sraz.service.PricingCalculator.Participant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Vyúčtování akce: potvrzení účasti, výpočet plateb (PricingCalculator), přebytek/schodek do banku
 * a evidence zaplacení. Po uzavření dostanou platící účastníci e-mail s částkou a platebními údaji.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AccountingService {

  private final EventRepository eventRepo;
  private final RegistrationRepository registrationRepo;
  private final ChargeRepository chargeRepo;
  private final BankEntryRepository bankRepo;
  private final UserRepository userRepo;
  private final AccessService access;
  private final AuditService audit;
  private final NotificationService notifications;
  private final Clock clock;

  /** Organizátor po akci potvrdí, kdo skutečně přišel (i nad rámec přihlášek přes registrationSet). */
  public Registration setAttendance(Long eventId, Long userId, boolean attended, User organizer) {
    Event event = findEvent(eventId);
    access.requireOrganizer(event.getGroup().getId(), organizer);
    requireOpen(event);
    Registration reg = registrationRepo.findByEventIdAndUserId(eventId, userId)
        .orElseThrow(() -> new DomainException("Účast lze potvrdit jen u přihlášeného člena – nejdřív ho přihlaste."));
    reg.setAttended(attended);
    audit.log(organizer, "ATTENDANCE_SET", "registration", reg.getId(), "attended=" + attended);
    return reg;
  }

  /** Omluví pozdní odhlášení / neúčast – člen pak nedostane pokutu. */
  public Registration setExcused(Long eventId, Long userId, boolean excused, User organizer) {
    Event event = findEvent(eventId);
    access.requireOrganizer(event.getGroup().getId(), organizer);
    requireOpen(event);
    Registration reg = registrationRepo.findByEventIdAndUserId(eventId, userId)
        .orElseThrow(() -> new NotFoundException("Přihláška", userId));
    reg.setExcused(excused);
    audit.log(organizer, "REGISTRATION_EXCUSED", "registration", reg.getId(), "excused=" + excused);
    return reg;
  }

  /**
   * Uzavře vyúčtování: účastníci = přihlášení (IN), kterým organizátor účast nezrušil. Vzniknou platby,
   * přebytek/schodek jde do banku a termín je DONE. Bez ceny se jen uzavře (akce zdarma).
   * Při zapnutých pokutách platí celý podíl i neomluveně pozdně odhlášení a nepřišlí – počítají se do dělitele.
   */
  public List<Charge> close(Long eventId, User organizer) {
    Event event = findEvent(eventId);
    Long groupId = event.getGroup().getId();
    access.requireOrganizer(groupId, organizer);
    requireOpen(event);
    OffsetDateTime now = OffsetDateTime.now(clock);
    if (event.getStatus()==EventStatus.CANCELLED) {
      throw new DomainException("Zrušenou akci nelze vyúčtovat.");
    }
    if (event.getStartsAt().isAfter(now)) {
      throw new DomainException("Vyúčtovat jde až akci, která už začala.");
    }

    List<Registration> registrations = registrationRepo.findByEventIdOrderByCreatedAt(eventId);
    List<Registration> attendees = registrations.stream()
        .filter(r -> r.getStatus()==RegistrationStatus.IN && !Boolean.FALSE.equals(r.getAttended()))
        .toList();
    attendees.forEach(r -> r.setAttended(true));

    event.setClosedAt(now);
    event.setStatus(EventStatus.DONE);
    if (event.getPricePerHour()==null) {
      audit.log(organizer, "EVENT_CLOSE", "event", eventId, "zdarma, účastníků " + attendees.size());
      return List.of();
    }

    Map<Long, ChargeReason> reasons = new LinkedHashMap<>();
    attendees.forEach(r -> reasons.put(r.getUser().getId(), ChargeReason.PLAYED));
    if (event.getGroup().isFinesEnabled()) {
      registrations.stream()
          .filter(r -> !r.isExcused())
          .forEach(r -> fineReason(r).ifPresent(reason -> reasons.put(r.getUser().getId(), reason)));
    }
    Map<Long, Registration> byUser = registrations.stream()
        .collect(Collectors.toMap(r -> r.getUser().getId(), r -> r));
    List<Participant> participants = reasons.keySet().stream()
        .map(userId -> new Participant(userId, kindOf(byUser.get(userId), groupId)))
        .toList();
    PricingCalculator.Result result = PricingCalculator.calculate(event.getPricePerHour(), event.getDurationMinutes(),
        event.getRegularFee(), participants);

    List<Charge> charges = participants.stream()
        .filter(p -> result.amounts().get(p.userId()).signum() > 0)
        .map(p -> chargeRepo.save(Charge.builder()
            .event(event)
            .user(userRepo.getReferenceById(p.userId()))
            .kind(p.kind())
            .reason(reasons.get(p.userId()))
            .amount(result.amounts().get(p.userId()))
            .build()))
        .toList();

    bankRepo.save(BankEntry.builder()
        .group(event.getGroup())
        .kind(BankEntryKind.EVENT)
        .amount(result.surplus())
        .event(event)
        .createdBy(organizer)
        .description("Vyúčtování %s: vybráno %s Kč, cena %s Kč".formatted(event.getName(),
            result.collected().stripTrailingZeros().toPlainString(), result.cost().stripTrailingZeros().toPlainString()))
        .build());

    for (Charge charge : charges) {
      try {
        notifications.sendCharge(charge);
      } catch (RuntimeException e) {
        log.error("close :: e-mail s platbou se nepodařilo odeslat: {}", charge.getUser().getEmail(), e);
      }
    }
    audit.log(organizer, "EVENT_CLOSE", "event", eventId,
        "účastníků %d, pokut %d, podíl %s, přebytek %s".formatted(attendees.size(),
            participants.size() - attendees.size(), result.share(), result.surplus()));
    return charges;
  }

  /** Znovu otevře vyúčtování (oprava účasti) – jen dokud nikdo nezaplatil. */
  public Event reopen(Long eventId, User organizer) {
    Event event = findEvent(eventId);
    access.requireOrganizer(event.getGroup().getId(), organizer);
    if (event.getClosedAt()==null) {
      return event;
    }
    List<Charge> charges = chargeRepo.findByEventIdOrderById(eventId);
    if (charges.stream().anyMatch(Charge::isPaid)) {
      throw new DomainException("Někdo už zaplatil – vyúčtování nejde znovu otevřít. Zrušte nejdřív označení platby.");
    }
    chargeRepo.deleteAll(charges);
    bankRepo.deleteAll(bankRepo.findByEventIdAndKind(eventId, BankEntryKind.EVENT));
    event.setClosedAt(null);
    event.setStatus(EventStatus.LOCKED);
    audit.log(organizer, "EVENT_REOPEN", "event", eventId, null);
    return event;
  }

  public Charge setPaid(Long chargeId, boolean paid, PaymentMethod method, User organizer) {
    Charge charge = chargeRepo.findById(chargeId).orElseThrow(() -> new NotFoundException("Platba", chargeId));
    access.requireOrganizer(charge.getEvent().getGroup().getId(), organizer);
    charge.setPaidAt(paid ? OffsetDateTime.now(clock):null);
    charge.setPaidMethod(paid ? (method!=null ? method:PaymentMethod.CASH):null);
    audit.log(organizer, "CHARGE_PAID", "charge", chargeId, "paid=%s method=%s".formatted(paid, charge.getPaidMethod()));
    return charge;
  }

  /** Platby termínu: organizátor vidí všechny, člen jen svou. */
  @Transactional(readOnly = true)
  public List<Charge> eventCharges(Event event, User user) {
    access.requireReader(event.getGroup().getId(), user);
    List<Charge> charges = chargeRepo.findByEventIdOrderById(event.getId());
    return access.isOrganizer(event.getGroup().getId(), user) ? charges
        :charges.stream().filter(c -> c.getUser().getId().equals(user.getId())).toList();
  }

  @Transactional(readOnly = true)
  public List<Charge> myCharges(User user) {
    return chargeRepo.findByUserIdOrderByCreatedAtDesc(user.getId());
  }

  /** Pokuta: odhlášen po uzávěrce, nebo přihlášen a organizátor potvrdil, že nepřišel. */
  private static Optional<ChargeReason> fineReason(Registration r) {
    if (r.getStatus()==RegistrationStatus.OUT && r.isLateCancel()) {
      return Optional.of(ChargeReason.LATE_CANCEL);
    }
    if (r.getStatus()==RegistrationStatus.IN && Boolean.FALSE.equals(r.getAttended())) {
      return Optional.of(ChargeReason.NO_SHOW);
    }
    return Optional.empty();
  }

  private ChargeKind kindOf(Registration r, Long groupId) {
    if (r.getPosition()==Position.GOALIE) {
      return ChargeKind.GOALIE;
    }
    return access.activeMembership(groupId, r.getUser())
        .filter(m -> m.getMemberType()==MemberType.REGULAR)
        .map(m -> ChargeKind.REGULAR)
        .orElse(ChargeKind.SUBSTITUTE);
  }

  private void requireOpen(Event event) {
    if (event.getClosedAt()!=null) {
      throw new DomainException("Vyúčtování je uzavřené – nejdřív ho znovu otevřete.");
    }
  }

  private Event findEvent(Long eventId) {
    return eventRepo.findById(eventId).orElseThrow(() -> new NotFoundException("Akce", eventId));
  }

  /** Pro zobrazení: částka z plateb celkem. */
  static BigDecimal total(List<Charge> charges) {
    return charges.stream().map(Charge::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
