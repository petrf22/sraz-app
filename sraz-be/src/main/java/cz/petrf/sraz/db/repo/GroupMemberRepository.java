package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.GroupMember;
import cz.petrf.sraz.db.entity.MemberType;
import cz.petrf.sraz.db.entity.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

  List<GroupMember> findByGroupIdOrderByEmail(Long groupId);

  List<GroupMember> findByGroupIdAndStatusAndMemberType(Long groupId, MembershipStatus status, MemberType memberType);

  Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

  Optional<GroupMember> findByGroupIdAndEmailIgnoreCase(Long groupId, String email);

  Optional<GroupMember> findByInviteToken(String inviteToken);

  List<GroupMember> findByEmailIgnoreCaseAndStatus(String email, MembershipStatus status);
}
