package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.SeriesPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeriesPeriodRepository extends JpaRepository<SeriesPeriod, Long> {
  List<SeriesPeriod> findBySeriesIdOrderByValidFrom(Long seriesId);
}
