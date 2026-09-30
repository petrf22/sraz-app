package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.BankEntry;
import cz.petrf.sraz.db.entity.BankEntryKind;
import cz.petrf.sraz.db.entity.User;
import cz.petrf.sraz.db.repo.BankEntryRepository;
import cz.petrf.sraz.exception.DomainException;
import cz.petrf.sraz.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bank skupiny – zůstatek z přebytků akcí a ručních pohybů (např. vánoční akce −, doplatek brankáře +).
 * Členové ho vidí, pohyby zapisuje organizátor.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BankService {

  private final BankEntryRepository bankRepo;
  private final GroupService groupService;
  private final AccessService access;
  private final AuditService audit;

  @Transactional(readOnly = true)
  public BigDecimal balance(Long groupId, User user) {
    access.requireReader(groupId, user);
    return bankRepo.balance(groupId);
  }

  @Transactional(readOnly = true)
  public List<BankEntry> entries(Long groupId, User user) {
    access.requireReader(groupId, user);
    return bankRepo.findByGroupIdOrderByCreatedAtDescIdDesc(groupId);
  }

  public BankEntry addManual(Long groupId, BigDecimal amount, String description, User organizer) {
    access.requireOrganizer(groupId, organizer);
    if (amount==null || amount.signum()==0) {
      throw new DomainException("Částka nesmí být nulová (příjem kladně, výdaj záporně).");
    }
    BankEntry entry = bankRepo.save(BankEntry.builder()
        .group(groupService.find(groupId))
        .kind(BankEntryKind.MANUAL)
        .amount(amount)
        .description(GroupService.requireText(description, "Popis pohybu"))
        .createdBy(organizer)
        .build());
    audit.log(organizer, "BANK_ADD", "bank_entry", entry.getId(), amount + " " + entry.getDescription());
    return entry;
  }

  /** Mazat jde jen ruční pohyby – pohyby z akcí se ruší znovuotevřením vyúčtování. */
  public void deleteManual(Long entryId, User organizer) {
    BankEntry entry = bankRepo.findById(entryId).orElseThrow(() -> new NotFoundException("Pohyb", entryId));
    access.requireOrganizer(entry.getGroup().getId(), organizer);
    if (entry.getKind()!=BankEntryKind.MANUAL) {
      throw new DomainException("Pohyb z vyúčtování akce se ruší znovuotevřením vyúčtování.");
    }
    bankRepo.delete(entry);
    audit.log(organizer, "BANK_DELETE", "bank_entry", entryId, entry.getAmount() + " " + entry.getDescription());
  }
}
