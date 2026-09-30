package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.Charge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChargeRepository extends JpaRepository<Charge, Long> {

  List<Charge> findByEventIdOrderById(Long eventId);

  List<Charge> findByUserIdOrderByCreatedAtDesc(Long userId);

  List<Charge> findByEventGroupIdAndPaidAtIsNullOrderByIdDesc(Long groupId);
}
