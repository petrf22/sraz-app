package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.db.repo.RegistrationRepository;
import cz.petrf.sraz.exception.DomainException;
import cz.petrf.sraz.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class AccountingServiceTest extends ServiceTestSupport {

  @Autowired
  AccountingService accounting;
  @Autowired
  BankService bank;
  @Autowired
  RegistrationService registrations;
  @Autowired
  RegistrationRepository registrationRepo;

  User organizer;
  SportGroup group;
  List<User> regulars = new ArrayList<>();
  List<User> substitutes = new ArrayList<>();
  User goalie;

  @BeforeEach
  void setUp() {
    organizer = user("Org");
    group = groupService.create("Večerní hokej", null, organizer);
    for (int i = 0; i < 9; i++) {           // + organizátor = 10 stálých
      User u = user("Stálý" + i);
      member(group, u, MemberType.REGULAR, Position.PLAYER);
      regulars.add(u);
    }
    regulars.add(organizer);
    for (int i = 0; i < 10; i++) {
      User u = user("Náhradník" + i);
      member(group, u, MemberType.SUBSTITUTE, Position.PLAYER);
      substitutes.add(u);
    }
    goalie = user("Brankář");
    member(group, goalie, MemberType.REGULAR, Position.GOALIE);
  }

  /** Akce s cenou 2800 Kč/h a poplatkem 200, už začala (lze vyúčtovat). */
  private Event startedEvent(List<User> players) {
    Event event = eventService.create(group.getId(), EventService.EventData.builder()
        .name("Hokej")
        .startsAt(OffsetDateTime.now().plusDays(2))
        .maxPlayersPerTeam(20)
        .pricePerHour(new BigDecimal("2800"))
        .regularFee(new BigDecimal("200"))
        .build(), organizer);
    for (User p : players) {
      registrations.respond(event.getId(), p, RegistrationStatus.IN, null, RegistrationSource.WEB);
    }
    event.setStartsAt(OffsetDateTime.now().minusMinutes(90));
    return event;
  }

  private BigDecimal amountFor(List<Charge> charges, User user) {
    return charges.stream().filter(c -> c.getUser().getId().equals(user.getId())).findFirst()
        .map(Charge::getAmount).orElse(null);
  }

  @Test
  void twentyPlayersAndGoalieRegularsPay200SubstitutesShareSurplusToBank() {
    List<User> all = new ArrayList<>(regulars);
    all.addAll(substitutes);
    all.add(goalie);
    Event event = startedEvent(all);

    List<Charge> charges = accounting.close(event.getId(), organizer);

    assertThat(charges).hasSize(20);                                  // brankář nic neplatí
    assertThat(amountFor(charges, regulars.getFirst())).isEqualByComparingTo("200");
    assertThat(amountFor(charges, substitutes.getFirst())).isEqualByComparingTo("140");
    assertThat(amountFor(charges, goalie)).isNull();
    assertThat(bank.balance(group.getId(), organizer)).isEqualByComparingTo("600");
    assertThat(event.getStatus()).isEqualTo(EventStatus.DONE);
    verify(emailService).sendHtmlEmail(anyString(), eq(substitutes.getFirst().getEmail()), anyString(), anyString());
  }

  @Test
  void playerMarkedAbsentDoesNotPay() {
    Event event = startedEvent(substitutes);
    accounting.setAttendance(event.getId(), substitutes.getFirst().getId(), false, organizer);

    List<Charge> charges = accounting.close(event.getId(), organizer);

    assertThat(charges).hasSize(9);
    assertThat(amountFor(charges, substitutes.getFirst())).isNull();
    assertThat(amountFor(charges, substitutes.get(1))).isEqualByComparingTo("320");   // 2800 / 9 → 320
  }

  @Test
  void cannotCloseBeforeTheEventStarts() {
    Event event = startedEvent(substitutes);
    event.setStartsAt(OffsetDateTime.now().plusHours(1));

    assertThatThrownBy(() -> accounting.close(event.getId(), organizer))
        .isInstanceOf(DomainException.class).hasMessageContaining("začala");
  }

  @Test
  void reopenRemovesChargesAndBankEntryUnlessSomeonePaid() {
    Event event = startedEvent(substitutes);
    List<Charge> charges = accounting.close(event.getId(), organizer);

    accounting.setPaid(charges.getFirst().getId(), true, PaymentMethod.CASH, organizer);
    assertThatThrownBy(() -> accounting.reopen(event.getId(), organizer)).isInstanceOf(DomainException.class);

    accounting.setPaid(charges.getFirst().getId(), false, null, organizer);
    accounting.reopen(event.getId(), organizer);

    assertThat(accounting.eventCharges(event, organizer)).isEmpty();
    assertThat(bank.balance(group.getId(), organizer)).isEqualByComparingTo("0");
    assertThat(event.getClosedAt()).isNull();
  }

  @Test
  void freeEventIsJustClosed() {
    Event event = startedEvent(substitutes);
    event.setPricePerHour(null);

    assertThat(accounting.close(event.getId(), organizer)).isEmpty();
    assertThat(event.getStatus()).isEqualTo(EventStatus.DONE);
    assertThat(bank.entries(group.getId(), organizer)).isEmpty();
  }

  @Test
  void memberSeesOnlyOwnCharge() {
    Event event = startedEvent(substitutes);
    accounting.close(event.getId(), organizer);

    assertThat(accounting.eventCharges(event, substitutes.getFirst())).hasSize(1);
    assertThat(accounting.eventCharges(event, organizer)).hasSize(10);
    assertThat(accounting.myCharges(substitutes.getFirst())).hasSize(1);
  }

  @Test
  void manualBankEntriesAndOnlyOrganizerWrites() {
    bank.addManual(group.getId(), new BigDecimal("-1500"), "Vánoční posezení", organizer);
    bank.addManual(group.getId(), new BigDecimal("140"), "Doplatek brankáře", organizer);

    assertThat(bank.balance(group.getId(), regulars.getFirst())).isEqualByComparingTo("-1360");
    assertThatThrownBy(() -> bank.addManual(group.getId(), BigDecimal.TEN, "x", regulars.getFirst()))
        .isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> bank.addManual(group.getId(), BigDecimal.ZERO, "x", organizer))
        .isInstanceOf(DomainException.class);
  }

  @Test
  void eventEntryInBankCannotBeDeletedDirectly() {
    Event event = startedEvent(substitutes);
    accounting.close(event.getId(), organizer);
    BankEntry entry = bank.entries(group.getId(), organizer).getFirst();

    assertThatThrownBy(() -> bank.deleteManual(entry.getId(), organizer)).isInstanceOf(DomainException.class);
  }

  @Test
  void closedEventCannotBeEdited() {
    Event event = startedEvent(substitutes);
    accounting.close(event.getId(), organizer);

    assertThatThrownBy(() -> eventService.update(event.getId(), EventService.EventData.builder()
        .name("x").startsAt(event.getStartsAt()).build(), organizer))
        .isInstanceOf(DomainException.class);
  }
}
