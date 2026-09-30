package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.BankTransaction;
import cz.petrf.sraz.db.entity.BankTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

  boolean existsByGroupIdAndFioId(Long groupId, Long fioId);

  List<BankTransaction> findByGroupIdOrderByBookedOnDescIdDesc(Long groupId);

  List<BankTransaction> findByGroupIdAndStatusOrderByBookedOnDescIdDesc(Long groupId, BankTransactionStatus status);

  List<BankTransaction> findByChargeId(Long chargeId);
}
