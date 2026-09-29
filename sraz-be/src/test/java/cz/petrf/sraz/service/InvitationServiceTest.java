package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class InvitationServiceTest extends ServiceTestSupport {

  @Autowired
  InvitationService invitations;

  User organizer;
  SportGroup group;
  User regular;
  User substitute;

  @BeforeEach
  void setUp() {
    organizer = user("Org");
    group = groupService.create("Večerní hokej", null, organizer);
    regular = user("Regular");
    substitute = user("Sub");
    member(group, regular, MemberType.REGULAR, Position.PLAYER);
    member(group, substitute, MemberType.SUBSTITUTE, Position.PLAYER);
  }

  @Test
  void waveGoesOnlyToGivenMemberTypeAndIsNotRepeated() {
    Event event = event(group, organizer, 10, 2);

    int sent = invitations.sendWave(event, MemberType.REGULAR);

    // stálý člen + organizátor (zakladatel je stálý člen)
    assertThat(sent).isEqualTo(2);
    verify(emailService).sendHtmlEmail(anyString(), eq(regular.getEmail()), anyString(), anyString());
    verify(emailService, never()).sendHtmlEmail(anyString(), eq(substitute.getEmail()), anyString(), anyString());
    assertThat(event.getStatus()).isEqualTo(EventStatus.OPEN);
    assertThat(event.getRegularsInvitedAt()).isNotNull();

    assertThat(invitations.sendWave(event, MemberType.REGULAR)).isZero();
  }

  @Test
  void scheduledWavesRespectHoursBeforeStart() {
    Event event = event(group, organizer, 10, 2);   // za 3 dny; stálí 96 h předem, náhradníci 48 h
    invitations.sendDueWaves();

    assertThat(event.getRegularsInvitedAt()).isNotNull();
    assertThat(event.getSubstitutesInvitedAt()).isNull();
    verify(emailService, never()).sendHtmlEmail(anyString(), eq(substitute.getEmail()), anyString(), anyString());
  }

  @Test
  void tokenRegistersExactlyThePersonItBelongsTo() {
    Event event = event(group, organizer, 10, 2);
    Invitation invitation = invitations.getOrCreate(event, regular);

    Registration reg = invitations.respond(invitation.getToken(), RegistrationStatus.IN, null);

    assertThat(reg.getUser().getId()).isEqualTo(regular.getId());
    assertThat(reg.getSource()).isEqualTo(RegistrationSource.EMAIL_LINK);
    assertThat(invitation.getExpiresAt()).isEqualTo(event.getStartsAt());
  }

  @Test
  void viewMarksInvitationAsOpened() {
    Event event = event(group, organizer, 10, 2);
    Invitation invitation = invitations.getOrCreate(event, regular);

    InvitationService.InvitationView view = invitations.view(invitation.getToken());

    assertThat(view.invitation().getOpenedAt()).isNotNull();
    assertThat(view.signupOpen()).isTrue();
    assertThat(view.teams()).hasSize(2);
  }

  @Test
  void expiredTokenIsRejected() {
    Event event = event(group, organizer, 10, 2);
    Invitation invitation = invitations.getOrCreate(event, regular);
    invitation.setExpiresAt(OffsetDateTime.now().minusMinutes(1));

    assertThatThrownBy(() -> invitations.respond(invitation.getToken(), RegistrationStatus.IN, null))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("Platnost");
  }

  @Test
  void unknownTokenIsRejected() {
    assertThatThrownBy(() -> invitations.view("neexistuje")).isInstanceOf(DomainException.class);
  }
}
