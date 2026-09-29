package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.db.repo.EventRepository;
import cz.petrf.sraz.db.repo.RegistrationRepository;
import cz.petrf.sraz.exception.DomainException;
import cz.petrf.sraz.exception.NotFoundException;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Termíny akcí – zakládání, úpravy, zrušení a přehledy.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EventService {

  public static final int DEFAULT_DURATION_MINUTES = 60;
  public static final int DEFAULT_PLAYERS_PER_TEAM = 10;
  public static final int DEFAULT_GOALIES = 2;
  public static final int DEFAULT_INVITE_REGULARS_HOURS = 96;
  public static final int DEFAULT_INVITE_SUBSTITUTES_HOURS = 48;
  public static final int DEFAULT_DEADLINE_HOURS = 24;

  private final EventRepository eventRepo;
  private final RegistrationRepository registrationRepo;
  private final GroupService groupService;
  private final AccessService access;
  private final AuditService audit;
  private final NotificationService notifications;
  private final Clock clock;

  /** Vstupní data termínu; null hodnoty = výchozí nastavení. */
  @Builder
  public record EventData(String name, OffsetDateTime startsAt, Integer durationMinutes, Long venueId,
                          Integer maxPlayersPerTeam, Integer maxGoalies, OffsetDateTime signupDeadline,
                          Integer inviteRegularsHoursBefore, Integer inviteSubstitutesHoursBefore,
                          Integer reminderHoursBefore, String note) {
  }

  @Transactional(readOnly = true)
  public Event get(Long eventId, User user) {
    Event event = find(eventId);
    access.requireReader(event.getGroup().getId(), user);
    return event;
  }

  public Event find(Long eventId) {
    return eventRepo.findById(eventId).orElseThrow(() -> new NotFoundException("Akce", eventId));
  }

  @Transactional(readOnly = true)
  public List<Event> groupEvents(Long groupId, OffsetDateTime from, OffsetDateTime to, User user) {
    access.requireReader(groupId, user);
    OffsetDateTime now = OffsetDateTime.now(clock);
    return eventRepo.findByGroupIdAndStartsAtBetweenOrderByStartsAt(groupId,
        from!=null ? from:now.minusDays(30), to!=null ? to:now.plusYears(1));
  }

  /** Nadcházející termíny ve všech skupinách uživatele (od začátku dnešního dne). */
  @Transactional(readOnly = true)
  public List<Event> myUpcoming(User user) {
    return eventRepo.findUpcomingForUser(user.getId(), OffsetDateTime.now(clock).minusHours(12));
  }

  public Event create(Long groupId, EventData data, User user) {
    access.requireOrganizer(groupId, user);
    Event event = Event.builder()
        .group(groupService.find(groupId))
        .status(EventStatus.PLANNED)
        .build();
    apply(event, data, true);
    event = eventRepo.save(event);

    audit.log(user, "EVENT_CREATE", "event", event.getId(), event.getName() + " " + event.getStartsAt());
    return event;
  }

  public Event update(Long eventId, EventData data, User user) {
    Event event = find(eventId);
    access.requireOrganizer(event.getGroup().getId(), user);
    if (event.getStatus()==EventStatus.CANCELLED || event.getStatus()==EventStatus.DONE) {
      throw new DomainException("Zrušenou nebo ukončenou akci nelze upravovat.");
    }
    apply(event, data, false);
    // ručně upravený termín série už přegenerování období nepřepíše
    event.setDetached(event.getPeriod()!=null);

    audit.log(user, "EVENT_UPDATE", "event", event.getId(), event.getName() + " " + event.getStartsAt());
    return event;
  }

  /** Zrušení termínu – všem přihlášeným a čekajícím přijde e-mail. */
  public Event cancel(Long eventId, String reason, User user) {
    Event event = find(eventId);
    access.requireOrganizer(event.getGroup().getId(), user);
    if (event.getStatus()==EventStatus.CANCELLED) {
      return event;
    }
    event.setStatus(EventStatus.CANCELLED);

    registrationRepo.findByEventIdOrderByCreatedAt(eventId).stream()
        .filter(r -> r.getStatus()!=RegistrationStatus.OUT)
        .forEach(r -> {
          try {
            notifications.sendEventCancelled(event, r.getUser(), StringUtils.trimToNull(reason));
          } catch (RuntimeException e) {
            log.error("cancel :: e-mail o zrušení se nepodařilo odeslat: {}", r.getUser().getEmail(), e);
          }
        });

    audit.log(user, "EVENT_CANCEL", "event", event.getId(), reason);
    return event;
  }

  private void apply(Event event, EventData d, boolean create) {
    event.setName(GroupService.requireText(d.name(), "Název akce"));

    if (d.startsAt()==null) {
      throw new DomainException("Začátek akce je povinný údaj.");
    }
    if (create && !d.startsAt().isAfter(OffsetDateTime.now(clock))) {
      throw new DomainException("Začátek akce musí být v budoucnosti.");
    }
    event.setStartsAt(d.startsAt());
    event.setDurationMinutes(positive(d.durationMinutes(), DEFAULT_DURATION_MINUTES, "Délka akce"));
    event.setMaxPlayersPerTeam(positive(d.maxPlayersPerTeam(), DEFAULT_PLAYERS_PER_TEAM, "Počet hráčů na tým"));
    event.setMaxGoalies(notNegative(d.maxGoalies(), DEFAULT_GOALIES, "Počet brankářů"));
    event.setInviteRegularsHoursBefore(notNegative(d.inviteRegularsHoursBefore(), DEFAULT_INVITE_REGULARS_HOURS, "Pozvání stálých"));
    event.setInviteSubstitutesHoursBefore(notNegative(d.inviteSubstitutesHoursBefore(), DEFAULT_INVITE_SUBSTITUTES_HOURS, "Pozvání náhradníků"));

    OffsetDateTime deadline = d.signupDeadline()!=null ? d.signupDeadline():d.startsAt().minusHours(DEFAULT_DEADLINE_HOURS);
    if (deadline.isAfter(d.startsAt())) {
      throw new DomainException("Uzávěrka přihlášek musí být před začátkem akce.");
    }
    event.setSignupDeadline(deadline);
    event.setVenue(d.venueId()!=null ? groupService.findVenue(event.getGroup().getId(), d.venueId()):null);
    event.setNote(StringUtils.trimToNull(d.note()));
    if (d.reminderHoursBefore()!=null && d.reminderHoursBefore() < 0) {
      throw new DomainException("Připomínka nesmí být záporná.");
    }
    event.setReminderHoursBefore(d.reminderHoursBefore());
  }

  private static int positive(Integer value, int def, String field) {
    int v = value!=null ? value:def;
    if (v <= 0) {
      throw new DomainException(field + " musí být kladné číslo.");
    }
    return v;
  }

  private static int notNegative(Integer value, int def, String field) {
    int v = value!=null ? value:def;
    if (v < 0) {
      throw new DomainException(field + " nesmí být záporné.");
    }
    return v;
  }
}
