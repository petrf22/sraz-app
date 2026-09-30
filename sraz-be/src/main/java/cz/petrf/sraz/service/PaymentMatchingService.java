package cz.petrf.sraz.service;

import cz.petrf.sraz.config.FioProperties;
import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.db.repo.BankTransactionRepository;
import cz.petrf.sraz.db.repo.ChargeRepository;
import cz.petrf.sraz.db.repo.SportGroupRepository;
import cz.petrf.sraz.exception.DomainException;
import cz.petrf.sraz.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Párování plateb z Fio API: stáhne pohyby na účtu skupiny, uloží nové a příchozí platby spáruje
 * na Charge podle variabilního symbolu (= id platby). Co nejde spárovat, čeká na organizátora.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentMatchingService {

  private final SportGroupRepository groupRepo;
  private final BankTransactionRepository txRepo;
  private final ChargeRepository chargeRepo;
  private final FioClient fio;
  private final TokenCipher cipher;
  private final FioProperties properties;
  private final AccessService access;
  private final AuditService audit;
  private final Clock clock;

  @Value("${app.time-zone:Europe/Prague}")
  private String timeZone;

  public record SyncResult(int fetched, int created, int matched, int unmatched) {
  }

  /**
   * Uloží token (null/prázdný = odpojit). Nový token se hned ověří staženým výpisem – neplatný se neuloží.
   */
  public Optional<SyncResult> setToken(Long groupId, String token, User organizer) {
    access.requireOrganizer(groupId, organizer);
    SportGroup group = findGroup(groupId);
    String trimmed = StringUtils.trimToNull(token);
    if (trimmed==null) {
      group.setFioTokenEnc(null);
      group.setFioLastSyncAt(null);
      group.setFioLastError(null);
      audit.log(organizer, "FIO_TOKEN_REMOVE", "group", groupId, null);
      return Optional.empty();
    }
    List<FioClient.Transaction> transactions = fetch(trimmed);   // chyba = výjimka, token se neuloží
    group.setFioTokenEnc(cipher.encrypt(trimmed));
    audit.log(organizer, "FIO_TOKEN_SET", "group", groupId, null);
    return Optional.of(process(group, transactions));
  }

  /** Ruční synchronizace organizátorem. Chyba Fio API se uloží ke skupině a vrátí uživateli. */
  @Transactional(noRollbackFor = DomainException.class)
  public SyncResult sync(Long groupId, User organizer) {
    access.requireOrganizer(groupId, organizer);
    return sync(groupId);
  }

  /** Synchronizace z plánovače (bez uživatele). */
  @Transactional(noRollbackFor = DomainException.class)
  public SyncResult sync(Long groupId) {
    SportGroup group = findGroup(groupId);
    if (group.getFioTokenEnc()==null) {
      throw new DomainException("Skupina nemá nastavený token Fio API.");
    }
    List<FioClient.Transaction> transactions;
    try {
      transactions = fetch(cipher.decrypt(group.getFioTokenEnc()));
    } catch (DomainException e) {
      group.setFioLastError(e.getMessage());
      throw e;
    }
    return process(group, transactions);
  }

  @Transactional(readOnly = true)
  public List<Long> groupsWithToken() {
    return groupRepo.findByFioTokenEncIsNotNull().stream().map(SportGroup::getId).toList();
  }

  @Transactional(readOnly = true)
  public List<BankTransaction> transactions(Long groupId, BankTransactionStatus status, User organizer) {
    access.requireOrganizer(groupId, organizer);
    return status==null ? txRepo.findByGroupIdOrderByBookedOnDescIdDesc(groupId)
        :txRepo.findByGroupIdAndStatusOrderByBookedOnDescIdDesc(groupId, status);
  }

  /** Nezaplacené platby skupiny – kandidáti pro ruční spárování. */
  @Transactional(readOnly = true)
  public List<Charge> unpaidCharges(Long groupId, User organizer) {
    access.requireOrganizer(groupId, organizer);
    return chargeRepo.findByEventGroupIdAndPaidAtIsNullOrderByIdDesc(groupId);
  }

  /** Organizátor ručně spáruje pohyb s platbou (např. chybný VS). */
  public BankTransaction assign(Long txId, Long chargeId, User organizer) {
    BankTransaction tx = findTx(txId);
    Long groupId = tx.getGroup().getId();
    access.requireOrganizer(groupId, organizer);
    if (tx.getStatus()==BankTransactionStatus.MATCHED) {
      throw new DomainException("Pohyb už je spárovaný.");
    }
    if (tx.getAmount().signum() <= 0) {
      throw new DomainException("Spárovat jde jen příchozí platbu.");
    }
    Charge charge = chargeRepo.findById(chargeId)
        .filter(c -> c.getEvent().getGroup().getId().equals(groupId))
        .orElseThrow(() -> new NotFoundException("Platba", chargeId));
    if (charge.isPaid()) {
      throw new DomainException("Platba už je označená jako zaplacená.");
    }
    markPaid(tx, charge);
    tx.setNote(tx.getAmount().compareTo(charge.getAmount())!=0
        ? "Spárováno ručně, částka %s z %s Kč".formatted(plain(tx.getAmount()), plain(charge.getAmount()))
        :"Spárováno ručně");
    audit.log(organizer, "CHARGE_PAID_FIO", "charge", chargeId, "ručně, fioId=" + tx.getFioId());
    return tx;
  }

  /** Pohyb, který s platbami nesouvisí (vklad, jiná platba), organizátor odloží. */
  public BankTransaction ignore(Long txId, User organizer) {
    BankTransaction tx = findTx(txId);
    access.requireOrganizer(tx.getGroup().getId(), organizer);
    if (tx.getStatus()==BankTransactionStatus.MATCHED) {
      throw new DomainException("Spárovaný pohyb nejde ignorovat – nejdřív zrušte označení platby.");
    }
    tx.setStatus(BankTransactionStatus.IGNORED);
    return tx;
  }

  /** Organizátor zrušil označení platby – spárovaný pohyb se vrátí mezi nespárované. */
  public void unlinkCharge(Long chargeId) {
    for (BankTransaction tx : txRepo.findByChargeId(chargeId)) {
      tx.setCharge(null);
      tx.setStatus(BankTransactionStatus.UNMATCHED);
      tx.setNote("Označení platby zrušeno organizátorem");
    }
  }

  private List<FioClient.Transaction> fetch(String token) {
    LocalDate to = LocalDate.now(clock.withZone(ZoneId.of(timeZone)));
    return fio.transactions(token, to.minusDays(properties.getLookbackDays()), to);
  }

  private SyncResult process(SportGroup group, List<FioClient.Transaction> transactions) {
    int created = 0, matched = 0, unmatched = 0;
    for (FioClient.Transaction t : transactions) {
      if (txRepo.existsByGroupIdAndFioId(group.getId(), t.fioId())) {
        continue;
      }
      BankTransaction tx = txRepo.save(BankTransaction.builder()
          .group(group)
          .fioId(t.fioId())
          .bookedOn(t.bookedOn())
          .amount(t.amount())
          .currency(t.currency())
          .variableSymbol(StringUtils.truncate(t.variableSymbol(), 10))
          .counterAccount(StringUtils.truncate(t.counterAccount(), 100))
          .counterName(StringUtils.truncate(t.counterName(), 255))
          .message(StringUtils.truncate(t.message(), 500))
          .status(BankTransactionStatus.UNMATCHED)
          .build());
      created++;
      match(group.getId(), tx);
      switch (tx.getStatus()) {
        case MATCHED -> matched++;
        case UNMATCHED -> unmatched++;
        case IGNORED -> {
        }
      }
    }
    group.setFioLastSyncAt(OffsetDateTime.now(clock));
    group.setFioLastError(null);
    log.info("sync :: skupina {}: staženo {}, nových {}, spárováno {}, nespárováno {}", group.getId(),
        transactions.size(), created, matched, unmatched);
    return new SyncResult(transactions.size(), created, matched, unmatched);
  }

  private void match(Long groupId, BankTransaction tx) {
    if (tx.getAmount().signum() <= 0) {
      tx.setStatus(BankTransactionStatus.IGNORED);
      tx.setNote("Odchozí platba");
      return;
    }
    if (tx.getCurrency()!=null && !"CZK".equals(tx.getCurrency())) {
      tx.setNote("Platba v jiné měně (%s)".formatted(tx.getCurrency()));
      return;
    }
    Long chargeId = parseVs(tx.getVariableSymbol());
    Optional<Charge> found = chargeId==null ? Optional.empty()
        :chargeRepo.findById(chargeId).filter(c -> c.getEvent().getGroup().getId().equals(groupId));
    if (found.isEmpty()) {
      tx.setNote(tx.getVariableSymbol()==null ? "Bez variabilního symbolu":"Neznámý variabilní symbol");
      return;
    }
    Charge charge = found.get();
    if (charge.isPaid()) {
      tx.setNote("Platba %d už je zaplacená".formatted(charge.getId()));
      return;
    }
    if (tx.getAmount().compareTo(charge.getAmount()) < 0) {
      tx.setNote("Nižší částka: %s z %s Kč".formatted(plain(tx.getAmount()), plain(charge.getAmount())));
      return;
    }
    markPaid(tx, charge);
    if (tx.getAmount().compareTo(charge.getAmount()) > 0) {
      tx.setNote("Přeplatek %s Kč".formatted(plain(tx.getAmount().subtract(charge.getAmount()))));
    }
    audit.log(null, "CHARGE_PAID_FIO", "charge", charge.getId(), "fioId=" + tx.getFioId());
  }

  private void markPaid(BankTransaction tx, Charge charge) {
    charge.setPaidAt(tx.getBookedOn().atStartOfDay(ZoneId.of(timeZone)).toOffsetDateTime());
    charge.setPaidMethod(PaymentMethod.TRANSFER);
    tx.setCharge(charge);
    tx.setStatus(BankTransactionStatus.MATCHED);
  }

  /** VS = id platby; úvodní nuly a nečíselné VS se ignorují. */
  static Long parseVs(String vs) {
    String digits = StringUtils.stripStart(StringUtils.trimToEmpty(vs), "0");
    if (digits.isEmpty() || !StringUtils.isNumeric(digits) || digits.length() > 10) {
      return null;
    }
    return Long.parseLong(digits);
  }

  private static String plain(BigDecimal amount) {
    return amount.stripTrailingZeros().toPlainString();
  }

  private SportGroup findGroup(Long groupId) {
    return groupRepo.findById(groupId).orElseThrow(() -> new NotFoundException("Skupina", groupId));
  }

  private BankTransaction findTx(Long txId) {
    return txRepo.findById(txId).orElseThrow(() -> new NotFoundException("Pohyb", txId));
  }
}
