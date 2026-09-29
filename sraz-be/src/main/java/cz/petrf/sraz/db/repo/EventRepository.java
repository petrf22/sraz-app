package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.Event;
import cz.petrf.sraz.db.entity.EventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

  /** Zamkne řádek termínu – přihlášky se zpracují postupně a kapacita se nepřekročí. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Event e where e.id = :id")
  Optional<Event> findByIdForUpdate(Long id);

  List<Event> findByGroupIdAndStartsAtBetweenOrderByStartsAt(Long groupId, OffsetDateTime from, OffsetDateTime to);

  @Query("""
      select e from Event e
      where e.startsAt >= :from
        and exists (select 1 from GroupMember m where m.group = e.group and m.user.id = :userId and m.status = cz.petrf.sraz.db.entity.MembershipStatus.ACTIVE)
      order by e.startsAt
      """)
  List<Event> findUpcomingForUser(Long userId, OffsetDateTime from);

  /** Termíny, u kterých ještě neproběhlo rozeslání některé vlny pozvánek. */
  @Query("""
      select e from Event e
      where e.status in :statuses and e.startsAt > :now
        and (e.regularsInvitedAt is null or e.substitutesInvitedAt is null)
      """)
  List<Event> findPendingInvitations(Collection<EventStatus> statuses, OffsetDateTime now);
}
