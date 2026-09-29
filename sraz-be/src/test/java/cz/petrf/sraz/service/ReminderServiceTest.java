package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReminderServiceTest extends ServiceTestSupport {

  @Autowired
  ReminderService reminders;
  @Autowired
  RegistrationService registrations;

  User organizer;
  SportGroup group;
  User coming;
  User notComing;
  User silent;

  @BeforeEach
  void setUp() {
    organizer = user("Org");
    group = groupService.create("Večerní hokej", null, organizer);
    coming = user("Přijde");
    notComing = user("Nepřijde");
    silent = user("Mlčí");
    member(group, coming, MemberType.REGULAR, Position.PLAYER);
    member(group, notComing, MemberType.REGULAR, Position.PLAYER);
    member(group, silent, MemberType.REGULAR, Position.PLAYER);
  }

  private Event eventIn(int hours, Integer reminderHours) {
    return eventService.create(group.getId(), EventService.EventData.builder()
        .name("Hokej")
        .startsAt(OffsetDateTime.now().plusHours(hours))
        .signupDeadline(OffsetDateTime.now().plusHours(hours - 1))
        .reminderHoursBefore(reminderHours)
        .build(), organizer);
  }

  @Test
  void reminderGoesOnlyToRegisteredPlayersAndOnlyOnce() {
    Event event = eventIn(2, 3);
    registrations.respond(event.getId(), coming, RegistrationStatus.IN, null, RegistrationSource.WEB);
    registrations.respond(event.getId(), notComing, RegistrationStatus.OUT, null, RegistrationSource.WEB);

    assertThat(reminders.sendDueReminders()).isEqualTo(1);
    verify(emailService).sendHtmlEmail(anyString(), eq(coming.getEmail()), anyString(), anyString());
    verify(emailService, never()).sendHtmlEmail(anyString(), eq(notComing.getEmail()), anyString(), anyString());

    assertThat(reminders.sendDueReminders()).isZero();
  }

  @Test
  void reminderWaitsUntilItsTime() {
    Event event = eventIn(10, 3);
    registrations.respond(event.getId(), coming, RegistrationStatus.IN, null, RegistrationSource.WEB);

    assertThat(reminders.sendDueReminders()).isZero();
    assertThat(event.getReminderSentAt()).isNull();
  }

  @Test
  void deadlineSummaryGoesToOrganizersWithRosterAndMissingAnswers() {
    Event event = eventIn(2, null);
    registrations.respond(event.getId(), coming, RegistrationStatus.IN, null, RegistrationSource.WEB);
    registrations.respond(event.getId(), notComing, RegistrationStatus.OUT, null, RegistrationSource.WEB);
    event.setSignupDeadline(OffsetDateTime.now().minusMinutes(1));

    assertThat(reminders.sendDueDeadlineSummaries()).isEqualTo(1);

    ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
    verify(emailService).sendHtmlEmail(anyString(), eq(organizer.getEmail()), anyString(), html.capture());
    assertThat(html.getValue()).contains("Přijde").contains("Mlčí").doesNotContain("Nepřijde");

    assertThat(reminders.sendDueDeadlineSummaries()).isZero();
  }

  @Test
  void summaryListsRegularsWithoutAnswer() {
    Event event = eventIn(2, null);
    registrations.respond(event.getId(), coming, RegistrationStatus.IN, null, RegistrationSource.WEB);

    NotificationService.DeadlineSummary summary = reminders.summary(event);

    assertThat(summary.noResponse()).containsExactly("Mlčí", "Nepřijde", "Org");
    assertThat(summary.columns()).extracting(NotificationService.DeadlineSummary.Column::title)
        .containsExactly("Modří", "Červení", "Brankáři");
    assertThat(summary.columns().getFirst().names()).isEqualTo(List.of("Přijde"));
  }

  @Test
  void cancelledEventGetsNoReminder() {
    Event event = eventIn(2, 3);
    registrations.respond(event.getId(), coming, RegistrationStatus.IN, null, RegistrationSource.WEB);
    eventService.cancel(event.getId(), null, organizer);
    clearInvocations(emailService);

    assertThat(reminders.sendDueReminders()).isZero();
    verifyNoInteractions(emailService);
  }
}
