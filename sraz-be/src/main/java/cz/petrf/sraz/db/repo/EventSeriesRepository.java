package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.EventSeries;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventSeriesRepository extends JpaRepository<EventSeries, Long> {
  List<EventSeries> findByGroupIdOrderByName(Long groupId);
}
