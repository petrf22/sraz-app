package cz.petrf.sraz.controller;

import cz.petrf.sraz.controller.GraphqlInputs.EventInput;
import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.security.CurrentUserService;
import cz.petrf.sraz.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * GraphQL: termíny akcí, soupiska a přihlášky.
 */
@Controller
@RequiredArgsConstructor
public class EventGraphqlController {

  private final CurrentUserService currentUser;
  private final EventService eventService;
  private final RegistrationService registrationService;
  private final InvitationService invitationService;
  private final Clock clock;

  @QueryMapping
  public Event event(@Argument Long id) {
    return eventService.get(id, currentUser.requireUser());
  }

  @QueryMapping
  public List<Event> myUpcomingEvents() {
    return eventService.myUpcoming(currentUser.requireUser());
  }

  // ---- pole typu Event ----

  @SchemaMapping
  public boolean signupOpen(Event event) {
    return event.isSignupOpen(OffsetDateTime.now(clock));
  }

  @SchemaMapping
  public List<Registration> registrations(Event event) {
    return registrationService.roster(event.getId(), currentUser.requireUser());
  }

  @SchemaMapping
  public Registration myRegistration(Event event) {
    Long userId = currentUser.requireUser().getId();
    return registrationService.roster(event.getId(), currentUser.requireUser()).stream()
        .filter(r -> r.getUser().getId().equals(userId))
        .findFirst().orElse(null);
  }

  @SchemaMapping
  public NotificationService.EventSummary summary(Event event) {
    return registrationService.summary(event);
  }

  // ---- mutace ----

  @MutationMapping
  public Event eventCreate(@Argument Long groupId, @Argument EventInput input) {
    return eventService.create(groupId, toData(input), currentUser.requireUser());
  }

  @MutationMapping
  public Event eventUpdate(@Argument Long id, @Argument EventInput input) {
    return eventService.update(id, toData(input), currentUser.requireUser());
  }

  @MutationMapping
  public Event eventCancel(@Argument Long id, @Argument String reason) {
    return eventService.cancel(id, reason, currentUser.requireUser());
  }

  @MutationMapping
  public int eventSendInvitations(@Argument Long id, @Argument MemberType memberType) {
    return invitationService.sendNow(id, memberType, currentUser.requireUser());
  }

  @MutationMapping
  public Registration registrationRespond(@Argument Long eventId, @Argument RegistrationStatus status, @Argument Long teamId) {
    return registrationService.respond(eventId, currentUser.requireUser(), status, teamId, RegistrationSource.WEB);
  }

  @MutationMapping
  public Registration registrationSet(@Argument Long eventId, @Argument Long userId, @Argument RegistrationStatus status,
                                      @Argument Long teamId, @Argument Position position) {
    return registrationService.setByOrganizer(eventId, userId, status, teamId, position, currentUser.requireUser());
  }

  private static EventService.EventData toData(EventInput in) {
    return EventService.EventData.builder()
        .name(in.name())
        .startsAt(in.startsAt())
        .durationMinutes(in.durationMinutes())
        .venueId(in.venueId())
        .maxPlayersPerTeam(in.maxPlayersPerTeam())
        .maxGoalies(in.maxGoalies())
        .signupDeadline(in.signupDeadline())
        .inviteRegularsHoursBefore(in.inviteRegularsHoursBefore())
        .inviteSubstitutesHoursBefore(in.inviteSubstitutesHoursBefore())
        .reminderHoursBefore(in.reminderHoursBefore())
        .pricePerHour(GraphqlInputs.money(in.pricePerHour()))
        .regularFee(GraphqlInputs.money(in.regularFee()))
        .note(in.note())
        .build();
  }
}
