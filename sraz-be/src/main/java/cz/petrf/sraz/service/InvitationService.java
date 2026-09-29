package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.db.repo.EventRepository;
import cz.petrf.sraz.db.repo.GroupMemberRepository;
import cz.petrf.sraz.db.repo.InvitationRepository;
import cz.petrf.sraz.db.repo.RegistrationRepository;
import cz.petrf.sraz.db.repo.TeamRepository;
import cz.petrf.sraz.exception.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Pozvánky na termín. Každá pozvánka má unikátní token pro dvojici osoba × termín,
 * platný do začátku akce. Stálí členové a náhradníci se zvou v různém předstihu.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InvitationService {

  private final InvitationRepository invitationRepo;
  private final GroupMemberRepository memberRepo;
  private final RegistrationRepository registrationRepo;
  private final TeamRepository teamRepo;
  private final EventRepository eventRepo;
  private final EventService eventService;
  private final RegistrationService registrationService;
  private final AccessService access;
  private final AuditService audit;
  private final NotificationService notifications;
  private final Clock clock;

  /** Co vidí držitel tokenu na stránce pozvánky. */
  public record InvitationView(Invitation invitation, Event event, Optional<Registration> registration,
                               List<Team> teams, NotificationService.EventSummary summary, boolean signupOpen,
                               boolean expired) {
  }

  /** Ruční rozeslání vlny organizátorem („pozvat teď“). */
  public int sendNow(Long eventId, MemberType memberType, User organizer) {
    Event event = eventService.find(eventId);
    access.requireOrganizer(event.getGroup().getId(), organizer);
    int sent = sendWave(event, memberType);
    audit.log(organizer, "INVITATIONS_SEND", "event", eventId, memberType + " sent=" + sent);
    return sent;
  }

  /**
   * Rozešle pozvánky aktivním členům daného typu, kteří ji ještě nedostali a nejsou přihlášení.
   * Opakované volání nikomu nepošle e-mail dvakrát.
   */
  public int sendWave(Event event, MemberType memberType) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    if (event.getStatus()==EventStatus.CANCELLED || event.getStatus()==EventStatus.DONE || !event.getStartsAt().isAfter(now)) {
      throw new DomainException("Na tuto akci už nelze zvát.");
    }

    List<Team> teams = teamRepo.findByGroupIdOrderBySortOrderAscIdAsc(event.getGroup().getId());
    int sent = 0;

    for (GroupMember member : memberRepo.findByGroupIdAndStatusAndMemberType(event.getGroup().getId(), MembershipStatus.ACTIVE, memberType)) {
      User user = member.getUser();
      if (user==null || user.getBlockedAt()!=null) {
        continue;
      }
      Invitation invitation = getOrCreate(event, user);
      boolean responded = registrationRepo.findByEventIdAndUserId(event.getId(), user.getId()).isPresent();
      if (invitation.getSentAt()!=null || responded) {
        continue;
      }
      try {
        notifications.sendEventInvitation(invitation, teams, member.getPosition(), registrationService.summary(event));
        invitation.setSentAt(now);
        sent++;
      } catch (RuntimeException e) {
        log.error("sendWave :: pozvánku se nepodařilo odeslat: {}", user.getEmail(), e);
      }
    }

    if (memberType==MemberType.REGULAR) {
      event.setRegularsInvitedAt(now);
    } else {
      event.setSubstitutesInvitedAt(now);
    }
    if (event.getStatus()==EventStatus.PLANNED) {
      event.setStatus(EventStatus.OPEN);
    }
    log.info("sendWave :: event={} type={} odesláno={}", event.getId(), memberType, sent);
    return sent;
  }

  /** Vlny, jejichž čas nastal (volá plánovač). */
  public void sendDueWaves() {
    OffsetDateTime now = OffsetDateTime.now(clock);
    for (Event event : eventRepo.findPendingInvitations(List.of(EventStatus.PLANNED, EventStatus.OPEN), now)) {
      if (event.getRegularsInvitedAt()==null && !now.isBefore(event.getStartsAt().minusHours(event.getInviteRegularsHoursBefore()))) {
        sendWave(event, MemberType.REGULAR);
      }
      if (event.getSubstitutesInvitedAt()==null && !now.isBefore(event.getStartsAt().minusHours(event.getInviteSubstitutesHoursBefore()))) {
        sendWave(event, MemberType.SUBSTITUTE);
      }
    }
  }

  public Invitation getOrCreate(Event event, User user) {
    return invitationRepo.findByEventIdAndUserId(event.getId(), user.getId())
        .orElseGet(() -> invitationRepo.save(Invitation.builder()
            .event(event)
            .user(user)
            .token(UUID.randomUUID().toString())
            .expiresAt(event.getStartsAt())
            .build()));
  }

  /** Zobrazení pozvánky podle tokenu – první otevření se zaznamená. */
  public InvitationView view(String token) {
    Invitation invitation = findValid(token, false);
    OffsetDateTime now = OffsetDateTime.now(clock);
    if (invitation.getOpenedAt()==null) {
      invitation.setOpenedAt(now);
    }
    Event event = invitation.getEvent();
    return new InvitationView(invitation, event,
        registrationRepo.findByEventIdAndUserId(event.getId(), invitation.getUser().getId()),
        teamRepo.findByGroupIdOrderBySortOrderAscIdAsc(event.getGroup().getId()),
        registrationService.summary(event),
        event.isSignupOpen(now),
        invitation.isExpired(now));
  }

  /** Odpověď přes token z e-mailu (bez přihlášení do aplikace). */
  public Registration respond(String token, RegistrationStatus status, Long teamId) {
    Invitation invitation = findValid(token, true);
    return registrationService.respond(invitation.getEvent().getId(), invitation.getUser(), status, teamId,
        RegistrationSource.EMAIL_LINK);
  }

  private Invitation findValid(String token, boolean requireNotExpired) {
    Invitation invitation = invitationRepo.findByToken(token)
        .orElseThrow(() -> new DomainException("Pozvánka neexistuje."));
    if (requireNotExpired && invitation.isExpired(OffsetDateTime.now(clock))) {
      throw new DomainException("Platnost pozvánky skončila začátkem akce.");
    }
    return invitation;
  }
}
