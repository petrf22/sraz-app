package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
  List<Team> findByGroupIdOrderBySortOrderAscIdAsc(Long groupId);
}
