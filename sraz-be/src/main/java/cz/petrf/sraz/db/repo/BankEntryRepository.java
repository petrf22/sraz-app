package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.BankEntry;
import cz.petrf.sraz.db.entity.BankEntryKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface BankEntryRepository extends JpaRepository<BankEntry, Long> {

  List<BankEntry> findByGroupIdOrderByCreatedAtDescIdDesc(Long groupId);

  List<BankEntry> findByEventIdAndKind(Long eventId, BankEntryKind kind);

  @Query("select coalesce(sum(b.amount), 0) from BankEntry b where b.group.id = :groupId")
  BigDecimal balance(Long groupId);
}
