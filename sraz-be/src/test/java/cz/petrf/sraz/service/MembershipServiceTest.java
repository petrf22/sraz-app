package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.exception.DomainException;
import cz.petrf.sraz.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class MembershipServiceTest extends ServiceTestSupport {

  @Autowired
  MembershipService memberships;

  User organizer;
  SportGroup group;

  @BeforeEach
  void setUp() {
    organizer = user("Org");
    group = groupService.create("Večerní hokej", null, organizer);
  }

  @Test
  void invitedPersonWithoutAccountBecomesVerifiedMemberAfterAccepting() {
    GroupMember invite = memberships.invite(group.getId(), " Novy.Hrac@Example.com ", MemberType.SUBSTITUTE, Position.PLAYER, organizer);

    assertThat(invite.getStatus()).isEqualTo(MembershipStatus.INVITED);
    assertThat(invite.getUser()).isNull();
    assertThat(invite.getEmail()).isEqualTo("novy.hrac@example.com");
    verify(emailService).sendHtmlEmail(anyString(), eq("novy.hrac@example.com"), anyString(), anyString());

    GroupMember accepted = memberships.respondByToken(invite.getInviteToken(), true);

    assertThat(accepted.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    assertThat(accepted.getInviteToken()).isNull();
    assertThat(accepted.getUser().getEmailVerifiedAt()).isNotNull();
    assertThat(userRepo.findByEmail("novy.hrac@example.com")).isPresent();
  }

  @Test
  void existingUserSeesInviteAndCanDeclineInApp() {
    User existing = user("Existing");
    GroupMember invite = memberships.invite(group.getId(), existing.getEmail(), MemberType.REGULAR, Position.GOALIE, organizer);

    assertThat(memberships.pendingInvites(existing)).extracting(GroupMember::getId).contains(invite.getId());

    GroupMember declined = memberships.respondAsUser(invite.getId(), false, existing);
    assertThat(declined.getStatus()).isEqualTo(MembershipStatus.DECLINED);
    assertThat(declined.getUser()).isNull();
  }

  @Test
  void inviteCannotBeAnsweredByAnotherUser() {
    GroupMember invite = memberships.invite(group.getId(), "someone@example.com", null, null, organizer);

    assertThatThrownBy(() -> memberships.respondAsUser(invite.getId(), true, user("Other")))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void usedTokenIsRejected() {
    GroupMember invite = memberships.invite(group.getId(), "once@example.com", null, null, organizer);
    String token = invite.getInviteToken();
    memberships.respondByToken(token, true);

    assertThatThrownBy(() -> memberships.respondByToken(token, true)).isInstanceOf(DomainException.class);
  }

  @Test
  void onlyOrganizerCanInvite() {
    User regular = user("Regular");
    member(group, regular, MemberType.REGULAR, Position.PLAYER);

    assertThatThrownBy(() -> memberships.invite(group.getId(), "x@example.com", null, null, regular))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void lastOrganizerCannotLeave() {
    GroupMember orgMembership = memberRepo.findByGroupIdAndUserId(group.getId(), organizer.getId()).orElseThrow();

    assertThatThrownBy(() -> memberships.remove(orgMembership.getId(), organizer))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("organizátora");
  }

  @Test
  void invalidEmailIsRejected() {
    assertThatThrownBy(() -> memberships.invite(group.getId(), "not-an-email", null, null, organizer))
        .isInstanceOf(DomainException.class);
  }
}
