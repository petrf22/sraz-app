package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.db.repo.EventRepository;
import cz.petrf.sraz.db.repo.RegistrationRepository;
import cz.petrf.sraz.db.repo.TeamRepository;
import cz.petrf.sraz.db.repo.UserRepository;
import cz.petrf.sraz.exception.DomainException;
import cz.petrf.sraz.exception.ForbiddenException;
import cz.petrf.sraz.exception.NotFoundException;
import cz.petrf.sraz.service.NotificationService.EventSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Přihlášky na termín: kapacita týmů a brankářů, fronta a uzávěrka.
 * <p>
 * Do uzávěrky se hráči přihlašují a odhlašují sami; když se uvolní místo, postoupí
 * první čekající z fronty (se stejným týmem). Po uzávěrce mění přihlášky jen organizátor
 * a nic se neposouvá automaticky – organizátor má přehled a řídí to sám.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RegistrationService {

  private static final DateTimeFormatter DEADLINE = DateTimeFormatter.ofPattern("d. M. H:mm");

  private final RegistrationRepository registrationRepo;
  private final EventRepository eventRepo;
  private final TeamRepository teamRepo;
  private final UserRepository userRepo;
  private final AccessService access;
  private final AuditService audit;
  private final NotificationService notifications;
  private final Clock clock;

  @Value("${app.time-zone:Europe/Prague}")
  private String timeZone;

  @Transactional(readOnly = true)
  public List<Registration> roster(Long eventId, User user) {
    Event event = eventRepo.findById(eventId).orElseThrow(() -> new NotFoundException("Akce", eventId));
    access.requireReader(event.getGroup().getId(), user);
    return registrationRepo.findByEventIdOrderByCreatedAt(eventId);
  }

  /** Soupiska bez kontroly oprávnění – pro držitele platného tokenu pozvánky. */
  @Transactional(readOnly = true)
  public List<Registration> rosterUnchecked(Long eventId) {
    return registrationRepo.findByEventIdOrderByCreatedAt(eventId);
  }

  /**
   * Hráč se sám přihlásí (IN) nebo odhlásí (OUT). Při plné kapacitě skončí ve frontě (WAITLIST).
   */
  public Registration respond(Long eventId, User user, RegistrationStatus wanted, Long teamId, RegistrationSource source) {
    Event event = lock(eventId);
    GroupMember membership = access.requireActiveMember(event.getGroup().getId(), user);
    OffsetDateTime now = OffsetDateTime.now(clock);

    if (user.getBlockedAt()!=null) {
      throw new ForbiddenException("Účet je zablokovaný.");
    }
    if (!event.isSignupOpen(now)) {
      throw new DomainException(event.getStatus()==EventStatus.CANCELLED
          ? "Akce je zrušená."
          : "Přihlašování skončilo (uzávěrka %s). Změnu domluvte s organizátorem."
          .formatted(event.getSignupDeadline().atZoneSameInstant(ZoneId.of(timeZone)).format(DEADLINE)));
    }
    if (wanted==RegistrationStatus.WAITLIST) {
      throw new DomainException("Do fronty se nelze zapsat přímo – zvolte přihlášení.");
    }

    Registration reg = registrationRepo.findByEventIdAndUserId(eventId, user.getId())
        .orElseGet(() -> Registration.builder().event(event).user(user).build());
    Position position = membership.getPosition();
    Team team = wanted==RegistrationStatus.IN ? resolveTeam(event, position, teamId, user.getId()):reg.getTeam();

    return apply(event, reg, wanted, position, team, source, user, true, now);
  }

  /**
   * Organizátor nastaví přihlášku libovolnému členovi – i po uzávěrce a nad kapacitu.
   */
  public Registration setByOrganizer(Long eventId, Long userId, RegistrationStatus status, Long teamId,
                                     Position position, User organizer) {
    Event event = lock(eventId);
    access.requireOrganizer(event.getGroup().getId(), organizer);
    User player = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("Uživatel", userId));
    GroupMember membership = access.requireActiveMember(event.getGroup().getId(), player);

    Registration reg = registrationRepo.findByEventIdAndUserId(eventId, userId)
        .orElseGet(() -> Registration.builder().event(event).user(player).build());
    Position pos = position!=null ? position:membership.getPosition();
    Team team = teamId!=null ? findTeam(event, teamId):reg.getTeam();

    return apply(event, reg, status, pos, team, RegistrationSource.ORGANIZER, organizer, false, OffsetDateTime.now(clock));
  }

  private Registration apply(Event event, Registration reg, RegistrationStatus wanted, Position position, Team team,
                             RegistrationSource source, User actor, boolean checkCapacity, OffsetDateTime now) {
    RegistrationStatus prevStatus = reg.getStatus();
    Team prevTeam = reg.getTeam();
    Position prevPosition = reg.getPosition();
    boolean sameSpot = prevStatus==RegistrationStatus.IN && prevPosition==position && sameTeam(prevTeam, team);

    RegistrationStatus result = wanted;
    if (checkCapacity && wanted==RegistrationStatus.IN && !sameSpot && !hasSpace(event, position, team, reg.getUser().getId())) {
      if (prevStatus==RegistrationStatus.IN) {
        // už přihlášený hráč by přechodem do plného týmu přišel o místo
        throw new DomainException("Tým %s je plný.".formatted(team!=null ? team.getName():""));
      }
      result = RegistrationStatus.WAITLIST;
    }

    boolean keepQueue = prevStatus==RegistrationStatus.WAITLIST && result==RegistrationStatus.WAITLIST && sameTeam(prevTeam, team);
    reg.setQueuedAt(result!=RegistrationStatus.WAITLIST ? null:keepQueue ? reg.getQueuedAt():now);
    if (prevStatus==RegistrationStatus.IN && result==RegistrationStatus.OUT) {
      // odhlášení po uzávěrce = pozdní odhlášení (při zapnutých pokutách platí celý podíl)
      reg.setLateCancel(!event.isSignupOpen(now) && event.getStatus()!=EventStatus.CANCELLED);
    } else if (result!=RegistrationStatus.OUT) {
      reg.setLateCancel(false);
    }
    if (result==RegistrationStatus.IN && prevStatus!=RegistrationStatus.IN) {
      reg.setExcused(false);
    }
    reg.setStatus(result);
    reg.setPosition(position);
    reg.setTeam(team);
    reg.setSource(source);
    reg = registrationRepo.save(reg);

    audit.log(actor, "REGISTRATION_SET", "registration", reg.getId(),
        "user=%s status=%s team=%s position=%s source=%s".formatted(reg.getUser().getEmail(), result,
            team!=null ? team.getName():null, position, source));

    // uvolněné místo obsadí první z fronty – jen do uzávěrky
    if (prevStatus==RegistrationStatus.IN && !(result==RegistrationStatus.IN && sameTeam(prevTeam, team) && prevPosition==position)
        && event.isSignupOpen(now)) {
      promoteFromWaitlist(event, prevPosition, prevTeam);
    }
    return reg;
  }

  private void promoteFromWaitlist(Event event, Position position, Team team) {
    registrationRepo.findByEventIdAndStatusOrderByQueuedAtAscIdAsc(event.getId(), RegistrationStatus.WAITLIST).stream()
        .filter(r -> r.getPosition()==position)
        .filter(r -> position==Position.GOALIE || !hasTeams(event) || sameTeam(r.getTeam(), team))
        .filter(r -> hasSpace(event, position, r.getTeam(), r.getUser().getId()))
        .findFirst()
        .ifPresent(r -> {
          r.setStatus(RegistrationStatus.IN);
          r.setQueuedAt(null);
          audit.log(null, "REGISTRATION_PROMOTE", "registration", r.getId(), "user=" + r.getUser().getEmail());
          try {
            notifications.sendPromotedFromWaitlist(r);
          } catch (RuntimeException e) {
            log.error("promote :: e-mail se nepodařilo odeslat: {}", r.getUser().getEmail(), e);
          }
        });
  }

  /** Volné místo: hráči podle týmu (bez týmů = jeden společný limit), brankáři celkem. */
  boolean hasSpace(Event event, Position position, Team team, Long exceptUserId) {
    List<Registration> in = registrationRepo.findByEventIdOrderByCreatedAt(event.getId()).stream()
        .filter(r -> r.getStatus()==RegistrationStatus.IN && r.getPosition()==position)
        .filter(r -> !r.getUser().getId().equals(exceptUserId))
        .toList();

    if (position==Position.GOALIE) {
      return in.size() < event.getMaxGoalies();
    }
    long taken = hasTeams(event) ? in.stream().filter(r -> sameTeam(r.getTeam(), team)).count():in.size();
    return taken < event.getMaxPlayersPerTeam();
  }

  /** Zvolený tým; bez volby hráč dostane nejméně obsazený tým. Brankář tým mít nemusí. */
  private Team resolveTeam(Event event, Position position, Long teamId, Long userId) {
    if (teamId!=null) {
      return findTeam(event, teamId);
    }
    if (position==Position.GOALIE) {
      return null;
    }
    List<Registration> regs = registrationRepo.findByEventIdOrderByCreatedAt(event.getId());
    return teamRepo.findByGroupIdOrderBySortOrderAscIdAsc(event.getGroup().getId()).stream()
        .min(Comparator.comparingLong(t -> regs.stream()
            .filter(r -> r.getStatus()==RegistrationStatus.IN && r.getPosition()==Position.PLAYER)
            .filter(r -> !r.getUser().getId().equals(userId) && sameTeam(r.getTeam(), t))
            .count()))
        .orElse(null);
  }

  private Team findTeam(Event event, Long teamId) {
    return teamRepo.findById(teamId)
        .filter(t -> t.getGroup().getId().equals(event.getGroup().getId()))
        .orElseThrow(() -> new NotFoundException("Tým", teamId));
  }

  private boolean hasTeams(Event event) {
    return !teamRepo.findByGroupIdOrderBySortOrderAscIdAsc(event.getGroup().getId()).isEmpty();
  }

  private static boolean sameTeam(Team a, Team b) {
    return Objects.equals(a!=null ? a.getId():null, b!=null ? b.getId():null);
  }

  private Event lock(Long eventId) {
    return eventRepo.findByIdForUpdate(eventId).orElseThrow(() -> new NotFoundException("Akce", eventId));
  }

  @Transactional(readOnly = true)
  public EventSummary summary(Event event) {
    List<Registration> regs = registrationRepo.findByEventIdOrderByCreatedAt(event.getId());
    int teams = Math.max(1, teamRepo.findByGroupIdOrderBySortOrderAscIdAsc(event.getGroup().getId()).size());
    return new EventSummary(
        (int) regs.stream().filter(r -> r.getStatus()==RegistrationStatus.IN && r.getPosition()==Position.PLAYER).count(),
        event.getMaxPlayersPerTeam() * teams,
        (int) regs.stream().filter(r -> r.getStatus()==RegistrationStatus.IN && r.getPosition()==Position.GOALIE).count(),
        event.getMaxGoalies(),
        (int) regs.stream().filter(r -> r.getStatus()==RegistrationStatus.WAITLIST).count());
  }
}
