package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.db.repo.EventRepository;
import cz.petrf.sraz.db.repo.GroupMemberRepository;
import cz.petrf.sraz.db.repo.RegistrationRepository;
import cz.petrf.sraz.db.repo.TeamRepository;
import cz.petrf.sraz.service.NotificationService.DeadlineSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Připomínky přihlášeným před začátkem akce a souhrn organizátorům po uzávěrce přihlášek
 * (volá plánovač). Každá se pošle jen jednou – čas odeslání se ukládá k termínu.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReminderService {

  private static final List<EventStatus> ACTIVE = List.of(EventStatus.PLANNED, EventStatus.OPEN, EventStatus.LOCKED);

  private final EventRepository eventRepo;
  private final RegistrationRepository registrationRepo;
  private final GroupMemberRepository memberRepo;
  private final TeamRepository teamRepo;
  private final RegistrationService registrationService;
  private final NotificationService notifications;
  private final Clock clock;

  public int sendDueReminders() {
    OffsetDateTime now = OffsetDateTime.now(clock);
    int sent = 0;
    for (Event event : eventRepo.findPendingReminders(ACTIVE, now)) {
      if (now.isBefore(event.getStartsAt().minusHours(event.getReminderHoursBefore()))) {
        continue;
      }
      for (Registration r : registrationRepo.findByEventIdAndStatusOrderByQueuedAtAscIdAsc(event.getId(), RegistrationStatus.IN)) {
        try {
          notifications.sendReminder(r);
          sent++;
        } catch (RuntimeException e) {
          log.error("sendDueReminders :: připomínku se nepodařilo odeslat: {}", r.getUser().getEmail(), e);
        }
      }
      event.setReminderSentAt(now);
    }
    return sent;
  }

  public int sendDueDeadlineSummaries() {
    OffsetDateTime now = OffsetDateTime.now(clock);
    int sent = 0;
    for (Event event : eventRepo.findPendingDeadlineSummaries(ACTIVE, now)) {
      DeadlineSummary summary = summary(event);
      for (GroupMember organizer : memberRepo.findByGroupIdOrderByEmail(event.getGroup().getId())) {
        if (!organizer.isActive() || !organizer.isOrganizer() || organizer.getUser()==null) {
          continue;
        }
        try {
          notifications.sendDeadlineSummary(event, organizer.getUser(), summary);
          sent++;
        } catch (RuntimeException e) {
          log.error("sendDueDeadlineSummaries :: souhrn se nepodařilo odeslat: {}", organizer.getEmail(), e);
        }
      }
      event.setDeadlineSummarySentAt(now);
    }
    return sent;
  }

  DeadlineSummary summary(Event event) {
    List<Registration> regs = registrationRepo.findByEventIdOrderByCreatedAt(event.getId());
    List<Team> teams = teamRepo.findByGroupIdOrderBySortOrderAscIdAsc(event.getGroup().getId());
    List<Registration> inPlayers = regs.stream()
        .filter(r -> r.getStatus()==RegistrationStatus.IN && r.getPosition()==Position.PLAYER).toList();

    List<DeadlineSummary.Column> columns = new ArrayList<>();
    for (Team t : teams) {
      columns.add(new DeadlineSummary.Column(t.getName(), names(inPlayers.stream()
          .filter(r -> r.getTeam()!=null && Objects.equals(r.getTeam().getId(), t.getId())).toList())));
    }
    if (teams.isEmpty()) {
      columns.add(new DeadlineSummary.Column("Hráči", names(inPlayers)));
    }
    columns.add(new DeadlineSummary.Column("Brankáři", names(regs.stream()
        .filter(r -> r.getStatus()==RegistrationStatus.IN && r.getPosition()==Position.GOALIE).toList())));

    List<String> waitlist = names(registrationRepo.findByEventIdAndStatusOrderByQueuedAtAscIdAsc(event.getId(), RegistrationStatus.WAITLIST));

    Set<Long> responded = regs.stream().map(r -> r.getUser().getId()).collect(Collectors.toSet());
    List<String> noResponse = memberRepo.findByGroupIdAndStatusAndMemberType(event.getGroup().getId(), MembershipStatus.ACTIVE, MemberType.REGULAR)
        .stream()
        .filter(m -> m.getUser()!=null && !responded.contains(m.getUser().getId()))
        .map(m -> m.getUser().getPublicName())
        .sorted()
        .toList();

    return new DeadlineSummary(columns, waitlist, noResponse, registrationService.summary(event));
  }

  private static List<String> names(List<Registration> regs) {
    return regs.stream().map(r -> r.getUser().getPublicName()).toList();
  }
}
