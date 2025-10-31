package cz.petrf.sraz.db.repo;
import cz.petrf.sraz.db.entity.EventMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EventMemberRepository extends JpaRepository<EventMember, Long> {
    List<EventMember> findByOwnerId(Long ownerId);
    List<EventMember> findByEventId(Long eventId);
    List<EventMember> findByMemberId(Long memberId);
    Optional<EventMember> findByGuid(String guid);
    List<EventMember> findByEventIdAndAttendedTrue(Long eventId);
}