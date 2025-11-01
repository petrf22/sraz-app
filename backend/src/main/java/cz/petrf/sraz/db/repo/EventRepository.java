package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
  List<Event> findByOwnerId(Long ownerId);

  List<Event> findByOwnerIdAndActiveTrueAndDeletedAtNull(Long ownerId);

  Optional<Event> findByIdAndOwnerId(Long id, Long ownerId);
}