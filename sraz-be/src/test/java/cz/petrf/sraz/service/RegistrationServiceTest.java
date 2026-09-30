package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.exception.DomainException;
import cz.petrf.sraz.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;

import static cz.petrf.sraz.db.entity.RegistrationStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class RegistrationServiceTest extends ServiceTestSupport {

  @Autowired
  RegistrationService registrations;

  User organizer;
  SportGroup group;
  Team blue;
  Team red;

  @BeforeEach
  void setUp() {
    organizer = user("Org");
    group = groupService.create("Večerní hokej", null, organizer);
    List<Team> teams = groupService.teams(group.getId());
    blue = teams.get(0);
    red = teams.get(1);
  }

  private User player(String name) {
    User u = user(name);
    member(group, u, MemberType.REGULAR, Position.PLAYER);
    return u;
  }

  @Test
  void fullTeamGoesToWaitlistAndFreedSpotPromotesFirstInQueue() {
    Event event = event(group, organizer, 2, 2);
    User a = player("A"), b = player("B"), c = player("C"), d = player("D");

    assertThat(registrations.respond(event.getId(), a, IN, blue.getId(), RegistrationSource.WEB).getStatus()).isEqualTo(IN);
    assertThat(registrations.respond(event.getId(), b, IN, blue.getId(), RegistrationSource.WEB).getStatus()).isEqualTo(IN);
    assertThat(registrations.respond(event.getId(), c, IN, blue.getId(), RegistrationSource.WEB).getStatus()).isEqualTo(WAITLIST);
    assertThat(registrations.respond(event.getId(), d, IN, blue.getId(), RegistrationSource.WEB).getStatus()).isEqualTo(WAITLIST);

    registrations.respond(event.getId(), a, OUT, null, RegistrationSource.WEB);

    assertThat(statusOf(event, c)).isEqualTo(IN);
    assertThat(statusOf(event, d)).isEqualTo(WAITLIST);
    verify(emailService).sendHtmlEmail(anyString(), eq(c.getEmail()), anyString(), anyString());
  }

  @Test
  void waitlistOnlyPromotesPlayersWaitingForTheFreedTeam() {
    Event event = event(group, organizer, 1, 2);
    User a = player("A"), b = player("B"), c = player("C");

    registrations.respond(event.getId(), a, IN, blue.getId(), RegistrationSource.WEB);
    registrations.respond(event.getId(), b, IN, red.getId(), RegistrationSource.WEB);
    assertThat(registrations.respond(event.getId(), c, IN, red.getId(), RegistrationSource.WEB).getStatus()).isEqualTo(WAITLIST);

    registrations.respond(event.getId(), a, OUT, null, RegistrationSource.WEB);
    assertThat(statusOf(event, c)).isEqualTo(WAITLIST);

    registrations.respond(event.getId(), b, OUT, null, RegistrationSource.WEB);
    assertThat(statusOf(event, c)).isEqualTo(IN);
  }

  @Test
  void playerWithoutTeamChoiceGetsLeastFilledTeam() {
    Event event = event(group, organizer, 5, 2);
    registrations.respond(event.getId(), player("A"), IN, blue.getId(), RegistrationSource.WEB);

    Registration reg = registrations.respond(event.getId(), player("B"), IN, null, RegistrationSource.WEB);

    assertThat(reg.getTeam().getId()).isEqualTo(red.getId());
  }

  @Test
  void registeredPlayerCannotSwitchIntoFullTeam() {
    Event event = event(group, organizer, 1, 2);
    User a = player("A");
    registrations.respond(event.getId(), a, IN, blue.getId(), RegistrationSource.WEB);
    registrations.respond(event.getId(), player("B"), IN, red.getId(), RegistrationSource.WEB);

    assertThatThrownBy(() -> registrations.respond(event.getId(), a, IN, red.getId(), RegistrationSource.WEB))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("plný");
    assertThat(statusOf(event, a)).isEqualTo(IN);
  }

  @Test
  void goaliesHaveTheirOwnLimit() {
    Event event = event(group, organizer, 10, 1);
    User g1 = user("G1"), g2 = user("G2");
    member(group, g1, MemberType.REGULAR, Position.GOALIE);
    member(group, g2, MemberType.REGULAR, Position.GOALIE);

    assertThat(registrations.respond(event.getId(), g1, IN, null, RegistrationSource.WEB).getStatus()).isEqualTo(IN);
    assertThat(registrations.respond(event.getId(), g2, IN, null, RegistrationSource.WEB).getStatus()).isEqualTo(WAITLIST);
  }

  @Test
  void afterDeadlineOnlyOrganizerChangesAndNobodyIsPromoted() {
    Event event = event(group, organizer, 1, 2);
    User a = player("A"), b = player("B");
    registrations.respond(event.getId(), a, IN, blue.getId(), RegistrationSource.WEB);
    registrations.respond(event.getId(), b, IN, blue.getId(), RegistrationSource.WEB);
    event.setSignupDeadline(OffsetDateTime.now().minusMinutes(1));

    assertThatThrownBy(() -> registrations.respond(event.getId(), a, OUT, null, RegistrationSource.WEB))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("uzávěrka");

    registrations.setByOrganizer(event.getId(), a.getId(), OUT, null, null, organizer);
    assertThat(statusOf(event, a)).isEqualTo(OUT);
    assertThat(statusOf(event, b)).isEqualTo(WAITLIST);

    // organizátor smí přihlásit i nad kapacitu
    registrations.setByOrganizer(event.getId(), b.getId(), IN, red.getId(), null, organizer);
    assertThat(statusOf(event, b)).isEqualTo(IN);
  }

  @Test
  void onlyUnregistrationAfterDeadlineIsLateCancelAndReturnResetsIt() {
    Event event = event(group, organizer, 5, 2);
    User a = player("A"), b = player("B");
    registrations.respond(event.getId(), a, IN, blue.getId(), RegistrationSource.WEB);
    registrations.respond(event.getId(), b, IN, blue.getId(), RegistrationSource.WEB);
    registrations.respond(event.getId(), b, OUT, null, RegistrationSource.WEB);
    event.setSignupDeadline(OffsetDateTime.now().minusMinutes(1));

    Registration late = registrations.setByOrganizer(event.getId(), a.getId(), OUT, null, null, organizer);
    assertThat(late.isLateCancel()).isTrue();
    assertThat(registrations.roster(event.getId(), organizer).stream()
        .filter(r -> r.getUser().getId().equals(b.getId())).findFirst().orElseThrow().isLateCancel()).isFalse();

    Registration back = registrations.setByOrganizer(event.getId(), a.getId(), IN, blue.getId(), null, organizer);
    assertThat(back.isLateCancel()).isFalse();
  }

  @Test
  void nonMemberCannotRegister() {
    Event event = event(group, organizer, 5, 2);

    assertThatThrownBy(() -> registrations.respond(event.getId(), user("Stranger"), IN, null, RegistrationSource.WEB))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void regularMemberCannotUseOrganizerOverride() {
    Event event = event(group, organizer, 5, 2);
    User a = player("A");

    assertThatThrownBy(() -> registrations.setByOrganizer(event.getId(), a.getId(), IN, null, null, a))
        .isInstanceOf(ForbiddenException.class);
  }

  private RegistrationStatus statusOf(Event event, User user) {
    return registrations.roster(event.getId(), organizer).stream()
        .filter(r -> r.getUser().getId().equals(user.getId()))
        .findFirst().orElseThrow().getStatus();
  }
}
