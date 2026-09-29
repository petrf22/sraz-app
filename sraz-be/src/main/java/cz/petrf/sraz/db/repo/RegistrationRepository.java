package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.Registration;
import cz.petrf.sraz.db.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

  List<Registration> findByEventIdOrderByCreatedAt(Long eventId);

  Optional<Registration> findByEventIdAndUserId(Long eventId, Long userId);

  List<Registration> findByEventIdAndStatusOrderByQueuedAtAscIdAsc(Long eventId, RegistrationStatus status);
}
