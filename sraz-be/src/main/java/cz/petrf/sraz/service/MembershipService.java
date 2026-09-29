package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.db.repo.GroupMemberRepository;
import cz.petrf.sraz.db.repo.RoleRepository;
import cz.petrf.sraz.db.repo.UserRepository;
import cz.petrf.sraz.exception.DomainException;
import cz.petrf.sraz.exception.ForbiddenException;
import cz.petrf.sraz.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Členství ve skupině. Pozvaný člověk musí s členstvím souhlasit (odkaz v e-mailu
 * nebo potvrzení po přihlášení) – do té doby mu nechodí žádné pozvánky na akce.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MembershipService {

  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  private final GroupMemberRepository memberRepo;
  private final UserRepository userRepo;
  private final RoleRepository roleRepo;
  private final GroupService groupService;
  private final AccessService access;
  private final AuditService audit;
  private final NotificationService notifications;
  private final DisposableEmailService disposableEmailService;
  private final Clock clock;

  @Transactional(readOnly = true)
  public List<GroupMember> members(Long groupId, User user) {
    access.requireReader(groupId, user);
    return memberRepo.findByGroupIdOrderByEmail(groupId);
  }

  /** Pozvánky do skupin čekající na odpověď přihlášeného uživatele. */
  @Transactional(readOnly = true)
  public List<GroupMember> pendingInvites(User user) {
    return memberRepo.findByEmailIgnoreCaseAndStatus(user.getEmail(), MembershipStatus.INVITED);
  }

  public GroupMember invite(Long groupId, String rawEmail, MemberType memberType, Position position, User organizer) {
    access.requireOrganizer(groupId, organizer);
    String email = normalizeEmail(rawEmail);

    GroupMember member = memberRepo.findByGroupIdAndEmailIgnoreCase(groupId, email)
        .orElseGet(() -> GroupMember.builder().group(groupService.find(groupId)).email(email).build());

    if (member.isActive()) {
      throw new DomainException("%s už je členem skupiny.".formatted(email));
    }

    // nová i opakovaná pozvánka (po odmítnutí/odebrání) dostane nový token
    if (member.getStatus()!=MembershipStatus.INVITED || member.getInviteToken()==null) {
      member.setInviteToken(UUID.randomUUID().toString());
    }
    member.setMemberType(memberType!=null ? memberType:MemberType.SUBSTITUTE);
    member.setPosition(position!=null ? position:Position.PLAYER);
    member.setStatus(MembershipStatus.INVITED);
    member.setInvitedBy(organizer);
    member.setRespondedAt(null);
    member = memberRepo.save(member);

    notifications.sendGroupInvite(member, organizer);
    audit.log(organizer, "MEMBER_INVITE", "group_member", member.getId(), email);
    return member;
  }

  @Transactional(readOnly = true)
  public GroupMember findInvite(String token) {
    return memberRepo.findByInviteToken(token)
        .filter(m -> m.getStatus()==MembershipStatus.INVITED)
        .orElseThrow(() -> new DomainException("Pozvánka je neplatná nebo už byla vyřízena."));
  }

  /**
   * Odpověď přes odkaz z e-mailu. Kliknutí na odkaz ověřuje e-mail, takže
   * případný nový účet vzniká rovnou jako ověřený.
   */
  public GroupMember respondByToken(String token, boolean accept) {
    GroupMember member = findInvite(token);
    User user = accept ? findOrCreateUser(member.getEmail()):userRepo.findByEmail(member.getEmail()).orElse(null);
    return respond(member, user, accept);
  }

  /** Odpověď přihlášeného uživatele v aplikaci. */
  public GroupMember respondAsUser(Long memberId, boolean accept, User user) {
    GroupMember member = memberRepo.findById(memberId)
        .filter(m -> m.getStatus()==MembershipStatus.INVITED)
        .orElseThrow(() -> new NotFoundException("Pozvánka", memberId));

    if (!member.getEmail().equalsIgnoreCase(user.getEmail())) {
      throw new ForbiddenException("Pozvánka patří jinému uživateli.");
    }
    return respond(member, user, accept);
  }

  private GroupMember respond(GroupMember member, User user, boolean accept) {
    if (user!=null && user.getBlockedAt()!=null) {
      throw new ForbiddenException("Účet je zablokovaný.");
    }
    member.setStatus(accept ? MembershipStatus.ACTIVE:MembershipStatus.DECLINED);
    member.setUser(accept ? user:null);
    member.setInviteToken(null);
    member.setRespondedAt(OffsetDateTime.now(clock));

    audit.log(user, accept ? "MEMBER_ACCEPT":"MEMBER_DECLINE", "group_member", member.getId(), member.getEmail());
    return member;
  }

  public GroupMember update(Long memberId, MemberType memberType, Position position, Boolean organizer, User actor) {
    GroupMember member = find(memberId);
    Long groupId = member.getGroup().getId();
    access.requireOrganizer(groupId, actor);

    if (Boolean.FALSE.equals(organizer) && member.isOrganizer()) {
      requireAnotherOrganizer(member);
    }
    if (memberType!=null) {
      member.setMemberType(memberType);
    }
    if (position!=null) {
      member.setPosition(position);
    }
    if (organizer!=null) {
      member.setOrganizer(organizer);
    }

    audit.log(actor, "MEMBER_UPDATE", "group_member", member.getId(),
        "type=%s position=%s organizer=%s".formatted(member.getMemberType(), member.getPosition(), member.isOrganizer()));
    return member;
  }

  /** Odebrání člena organizátorem, nebo odchod ze skupiny (člen sám). */
  public GroupMember remove(Long memberId, User actor) {
    GroupMember member = find(memberId);
    boolean self = member.getUser()!=null && member.getUser().getId().equals(actor.getId());

    if (!self) {
      access.requireOrganizer(member.getGroup().getId(), actor);
    }
    if (member.isOrganizer() && member.isActive()) {
      requireAnotherOrganizer(member);
    }
    member.setStatus(MembershipStatus.REMOVED);
    member.setOrganizer(false);
    member.setInviteToken(null);

    audit.log(actor, self ? "MEMBER_LEAVE":"MEMBER_REMOVE", "group_member", member.getId(), member.getEmail());
    return member;
  }

  private void requireAnotherOrganizer(GroupMember member) {
    boolean another = memberRepo.findByGroupIdOrderByEmail(member.getGroup().getId()).stream()
        .anyMatch(m -> m.isActive() && m.isOrganizer() && !m.getId().equals(member.getId()));
    if (!another) {
      throw new DomainException("Skupina musí mít alespoň jednoho organizátora.");
    }
  }

  private GroupMember find(Long memberId) {
    return memberRepo.findById(memberId).orElseThrow(() -> new NotFoundException("Člen", memberId));
  }

  private User findOrCreateUser(String email) {
    return userRepo.findByEmail(email).map(u -> {
      if (u.getEmailVerifiedAt()==null) {
        u.setEmailVerifiedAt(OffsetDateTime.now(clock));
      }
      return u;
    }).orElseGet(() -> userRepo.save(User.builder()
        .publicName(StringUtils.substringBefore(email, "@"))
        .email(email)
        .emailVerifiedAt(OffsetDateTime.now(clock))
        .termsAcceptedAt(OffsetDateTime.now(clock))
        .roles(Set.of(roleRepo.findByName("ROLE_USER").orElseThrow()))
        .build()));
  }

  private String normalizeEmail(String rawEmail) {
    String email = StringUtils.trimToEmpty(rawEmail).toLowerCase(Locale.ROOT);
    if (!EMAIL.matcher(email).matches()) {
      throw new DomainException("Neplatný e-mail: " + rawEmail);
    }
    if (disposableEmailService.isDisposable(email)) {
      throw new DomainException("Doména e-mailu %s není povolená.".formatted(email));
    }
    return email;
  }
}
