package cz.petrf.sraz.db.repo;
import cz.petrf.sraz.db.entity.MemberLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MemberLabelRepository extends JpaRepository<MemberLabel, Long> {
    List<MemberLabel> findByOwnerId(Long ownerId);
    List<MemberLabel> findByMemberId(Long memberId);
    List<MemberLabel> findByLabelId(Long labelId);
}