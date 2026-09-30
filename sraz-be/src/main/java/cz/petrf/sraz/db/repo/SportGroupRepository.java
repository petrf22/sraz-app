package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.SportGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SportGroupRepository extends JpaRepository<SportGroup, Long> {

  @Query("""
      select g from SportGroup g
      where exists (select 1 from GroupMember m where m.group = g and m.user.id = :userId and m.status = cz.petrf.sraz.db.entity.MembershipStatus.ACTIVE)
      order by g.name
      """)
  List<SportGroup> findActiveForUser(Long userId);

  List<SportGroup> findByFioTokenEncIsNotNull();
}
