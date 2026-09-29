package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.GroupMember;
import cz.petrf.sraz.db.entity.User;
import cz.petrf.sraz.db.repo.GroupMemberRepository;
import cz.petrf.sraz.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Oprávnění ve skupině: člen (ACTIVE), organizátor, nebo globální administrátor.
 */
@Service
@RequiredArgsConstructor
public class AccessService {

  public static final String ROLE_ADMIN = "ROLE_ADMIN";

  private final GroupMemberRepository memberRepo;

  public boolean isAdmin(User user) {
    return user.getRoles().stream().anyMatch(r -> ROLE_ADMIN.equals(r.getName()));
  }

  public Optional<GroupMember> activeMembership(Long groupId, User user) {
    return memberRepo.findByGroupIdAndUserId(groupId, user.getId())
        .filter(GroupMember::isActive);
  }

  public boolean isOrganizer(Long groupId, User user) {
    return isAdmin(user) || activeMembership(groupId, user).map(GroupMember::isOrganizer).orElse(false);
  }

  public GroupMember requireActiveMember(Long groupId, User user) {
    return activeMembership(groupId, user)
        .orElseThrow(() -> new ForbiddenException("Nejste členem této skupiny."));
  }

  /** Člen nebo admin – smí číst data skupiny. */
  public void requireReader(Long groupId, User user) {
    if (!isAdmin(user)) {
      requireActiveMember(groupId, user);
    }
  }

  public void requireOrganizer(Long groupId, User user) {
    if (!isOrganizer(groupId, user)) {
      throw new ForbiddenException("Tuto akci může provést jen organizátor skupiny.");
    }
  }
}
