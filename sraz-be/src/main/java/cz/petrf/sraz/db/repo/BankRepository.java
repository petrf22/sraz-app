package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankRepository extends JpaRepository<Bank, Long> {
  List<Bank> findByOwnerId(Long ownerId);
}