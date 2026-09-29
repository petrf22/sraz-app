package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VenueRepository extends JpaRepository<Venue, Long> {
  List<Venue> findByGroupIdOrderByName(Long groupId);
}
