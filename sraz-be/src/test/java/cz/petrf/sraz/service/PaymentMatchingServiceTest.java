package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.db.repo.BankTransactionRepository;
import cz.petrf.sraz.db.repo.SportGroupRepository;
import cz.petrf.sraz.exception.DomainException;
import cz.petrf.sraz.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class PaymentMatchingServiceTest extends ServiceTestSupport {

  @MockitoBean
  FioClient fio;

  @Autowired
  PaymentMatchingService matching;
  @Autowired
  AccountingService accounting;
  @Autowired
  RegistrationService registrations;
  @Autowired
  BankTransactionRepository txRepo;
  @Autowired
  SportGroupRepository groupRepo;

  User organizer;
  User adam;
  User bara;
  SportGroup group;
  Charge adamCharge;
  Charge baraCharge;

  @BeforeEach
  void setUp() {
    organizer = user("Org");
    group = groupService.create("Hokej", null, organizer);
    adam = user("Adam");
    bara = user("Bára");
    member(group, adam, MemberType.SUBSTITUTE, Position.PLAYER);
    member(group, bara, MemberType.SUBSTITUTE, Position.PLAYER);
    Event event = eventService.create(group.getId(), EventService.EventData.builder().name("Hokej")
        .startsAt(OffsetDateTime.now().plusDays(2)).pricePerHour(new BigDecimal("600")).build(), organizer);
    registrations.respond(event.getId(), adam, RegistrationStatus.IN, null, RegistrationSource.WEB);
    registrations.respond(event.getId(), bara, RegistrationStatus.IN, null, RegistrationSource.WEB);
    event.setStartsAt(OffsetDateTime.now().minusHours(2));
    List<Charge> charges = accounting.close(event.getId(), organizer);   // 600 / 2 = 300 každý
    adamCharge = charges.stream().filter(c -> c.getUser().getId().equals(adam.getId())).findFirst().orElseThrow();
    baraCharge = charges.stream().filter(c -> c.getUser().getId().equals(bara.getId())).findFirst().orElseThrow();
  }

  private static FioClient.Transaction tx(long id, String amount, String vs) {
    return new FioClient.Transaction(id, LocalDate.of(2026, 9, 28), new BigDecimal(amount), "CZK", vs, "123/0800", "Plátce", null);
  }

  private void statement(FioClient.Transaction... transactions) {
    when(fio.transactions(eq("TOKEN"), any(), any())).thenReturn(List.of(transactions));
  }

  @Test
  void setTokenStoresEncryptedTokenAndMatchesByVariableSymbol() {
    statement(
        tx(1, "300", "000" + adamCharge.getId()),      // spáruje se, úvodní nuly nevadí
        tx(2, "200", baraCharge.getId().toString()),   // nižší částka
        tx(3, "500", "999999"),                        // cizí VS
        tx(4, "-1000", null));                         // odchozí

    PaymentMatchingService.SyncResult result = matching.setToken(group.getId(), " TOKEN ", organizer).orElseThrow();

    assertThat(result).isEqualTo(new PaymentMatchingService.SyncResult(4, 4, 1, 2));
    assertThat(group.getFioTokenEnc()).isNotBlank().doesNotContain("TOKEN");
    assertThat(adamCharge.isPaid()).isTrue();
    assertThat(adamCharge.getPaidMethod()).isEqualTo(PaymentMethod.TRANSFER);
    assertThat(baraCharge.isPaid()).isFalse();
    List<BankTransaction> unmatched = matching.transactions(group.getId(), BankTransactionStatus.UNMATCHED, organizer);
    assertThat(unmatched).extracting(BankTransaction::getNote)
        .containsExactlyInAnyOrder("Nižší částka: 200 z 300 Kč", "Neznámý variabilní symbol");
    assertThat(matching.transactions(group.getId(), BankTransactionStatus.IGNORED, organizer)).hasSize(1);
  }

  @Test
  void repeatedSyncDoesNotDuplicateAndAlreadyPaidIsReported() {
    statement(tx(1, "300", adamCharge.getId().toString()));
    matching.setToken(group.getId(), "TOKEN", organizer);

    statement(tx(1, "300", adamCharge.getId().toString()), tx(2, "300", adamCharge.getId().toString()));
    PaymentMatchingService.SyncResult result = matching.sync(group.getId(), organizer);

    assertThat(result.created()).isEqualTo(1);
    assertThat(result.unmatched()).isEqualTo(1);
    assertThat(matching.transactions(group.getId(), null, organizer)).hasSize(2);
    assertThat(group.getFioLastSyncAt()).isNotNull();
  }

  @Test
  void invalidTokenIsNotSavedAndSyncErrorIsStored() {
    when(fio.transactions(eq("BAD"), any(), any())).thenThrow(new DomainException("Fio API odmítlo požadavek (HTTP 500) – zkontroluj token."));

    assertThatThrownBy(() -> matching.setToken(group.getId(), "BAD", organizer)).isInstanceOf(DomainException.class);
    assertThat(group.getFioTokenEnc()).isNull();

    statement();
    matching.setToken(group.getId(), "TOKEN", organizer);
    when(fio.transactions(eq("TOKEN"), any(), any())).thenThrow(new DomainException("Fio API je nedostupné – zkus to později."));
    assertThatThrownBy(() -> matching.sync(group.getId(), organizer)).isInstanceOf(DomainException.class);
    assertThat(group.getFioLastError()).contains("nedostupné");
  }

  @Test
  void manualAssignIgnoreAndUnmarkPaidReturnsTransaction() {
    statement(tx(1, "300", "bez vs"), tx(2, "50", null));
    matching.setToken(group.getId(), "TOKEN", organizer);
    List<BankTransaction> txs = matching.transactions(group.getId(), BankTransactionStatus.UNMATCHED, organizer);
    BankTransaction payment = txs.stream().filter(t -> t.getFioId()==1L).findFirst().orElseThrow();
    BankTransaction other = txs.stream().filter(t -> t.getFioId()==2L).findFirst().orElseThrow();

    assertThat(matching.unpaidCharges(group.getId(), organizer)).extracting(Charge::getId)
        .containsExactlyInAnyOrder(adamCharge.getId(), baraCharge.getId());
    matching.assign(payment.getId(), baraCharge.getId(), organizer);
    matching.ignore(other.getId(), organizer);

    assertThat(baraCharge.isPaid()).isTrue();
    assertThat(payment.getStatus()).isEqualTo(BankTransactionStatus.MATCHED);
    assertThat(other.getStatus()).isEqualTo(BankTransactionStatus.IGNORED);
    assertThatThrownBy(() -> matching.ignore(payment.getId(), organizer)).isInstanceOf(DomainException.class);

    accounting.setPaid(baraCharge.getId(), false, null, organizer);
    assertThat(payment.getStatus()).isEqualTo(BankTransactionStatus.UNMATCHED);
    assertThat(payment.getCharge()).isNull();
  }

  @Test
  void onlyOrganizerManagesFio() {
    assertThatThrownBy(() -> matching.setToken(group.getId(), "TOKEN", adam)).isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> matching.transactions(group.getId(), null, adam)).isInstanceOf(ForbiddenException.class);
  }

  @Test
  void emptyTokenDisconnects() {
    statement();
    matching.setToken(group.getId(), "TOKEN", organizer);

    assertThat(matching.setToken(group.getId(), "", organizer)).isEmpty();
    assertThat(group.getFioTokenEnc()).isNull();
  }

  @Test
  void parsesVariableSymbol() {
    assertThat(PaymentMatchingService.parseVs("00042")).isEqualTo(42L);
    assertThat(PaymentMatchingService.parseVs("abc")).isNull();
    assertThat(PaymentMatchingService.parseVs("0")).isNull();
    assertThat(PaymentMatchingService.parseVs(null)).isNull();
  }
}
