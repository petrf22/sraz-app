package cz.petrf.sraz.db.repo;
import cz.petrf.sraz.db.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByOwnerId(Long ownerId);
    List<Member> findByOwnerIdAndDeletedAtNull(Long ownerId);
    Optional<Member> findByOwnerIdAndUserId(Long ownerId, Long userId);
    boolean existsByOwnerIdAndPublicName(Long ownerId, String publicName);
}