package cz.petrf.sraz.controller;

import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.exception.DomainException;
import cz.petrf.sraz.service.InvitationService;
import cz.petrf.sraz.service.InvitationService.InvitationView;
import cz.petrf.sraz.service.MembershipService;
import cz.petrf.sraz.service.NotificationService.EventSummary;
import cz.petrf.sraz.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Veřejné stránky otevírané odkazem z e-mailu – bez přihlášení do aplikace.
 * Token pozvánky na akci opravňuje jen k přihlášení/odhlášení držitele na tento jeden termín.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicTokenController {

  private final InvitationService invitationService;
  private final RegistrationService registrationService;
  private final MembershipService membershipService;

  // ---- pozvánka na akci ----

  public record VenueDto(String name, String address, String mapUrl, BigDecimal latitude, BigDecimal longitude) {
  }

  public record TeamDto(Long id, String name, String color) {
  }

  public record RosterEntryDto(String name, RegistrationStatus status, Position position, Long teamId) {
  }

  public record InvitationDto(String groupName, String eventName, OffsetDateTime startsAt, int durationMinutes,
                              OffsetDateTime signupDeadline, EventStatus eventStatus, String note, VenueDto venue,
                              String playerName, Position position, RegistrationStatus myStatus, Long myTeamId,
                              List<TeamDto> teams, EventSummary summary, List<RosterEntryDto> roster,
                              boolean signupOpen, boolean expired) {
  }

  public record RespondRequest(RegistrationStatus status, Long teamId) {
  }

  @GetMapping("/invitations/{token}")
  @Transactional
  public InvitationDto invitation(@PathVariable String token) {
    return toDto(invitationService.view(token));
  }

  @PostMapping("/invitations/{token}")
  @Transactional
  public InvitationDto respond(@PathVariable String token, @RequestBody RespondRequest request) {
    invitationService.respond(token, request.status(), request.teamId());
    return toDto(invitationService.view(token));
  }

  private InvitationDto toDto(InvitationView view) {
    Event event = view.event();
    Venue venue = event.getVenue();
    Registration mine = view.registration().orElse(null);
    List<RosterEntryDto> roster = registrationService.rosterUnchecked(event.getId()).stream()
        .filter(r -> r.getStatus()!=RegistrationStatus.OUT)
        .map(r -> new RosterEntryDto(r.getUser().getPublicName(), r.getStatus(), r.getPosition(),
            r.getTeam()!=null ? r.getTeam().getId():null))
        .toList();

    return new InvitationDto(
        event.getGroup().getName(), event.getName(), event.getStartsAt(), event.getDurationMinutes(),
        event.getSignupDeadline(), event.getStatus(), event.getNote(),
        venue!=null ? new VenueDto(venue.getName(), venue.getAddress(), venue.getMapUrl(), venue.getLatitude(), venue.getLongitude()):null,
        view.invitation().getUser().getPublicName(),
        mine!=null ? mine.getPosition():null,
        mine!=null ? mine.getStatus():null,
        mine!=null && mine.getTeam()!=null ? mine.getTeam().getId():null,
        view.teams().stream().map(t -> new TeamDto(t.getId(), t.getName(), t.getColor())).toList(),
        view.summary(), roster, view.signupOpen(), view.expired());
  }

  // ---- pozvánka do skupiny ----

  public record GroupInviteDto(String groupName, String invitedBy, String email, MemberType memberType, Position position) {
  }

  public record GroupInviteRespondRequest(boolean accept) {
  }

  @GetMapping("/group-invites/{token}")
  @Transactional(readOnly = true)
  public GroupInviteDto groupInvite(@PathVariable String token) {
    GroupMember m = membershipService.findInvite(token);
    return new GroupInviteDto(m.getGroup().getName(),
        m.getInvitedBy()!=null ? m.getInvitedBy().getPublicName():null,
        m.getEmail(), m.getMemberType(), m.getPosition());
  }

  @PostMapping("/group-invites/{token}")
  public Map<String, MembershipStatus> respondGroupInvite(@PathVariable String token, @RequestBody GroupInviteRespondRequest request) {
    return Map.of("status", membershipService.respondByToken(token, request.accept()).getStatus());
  }

  // ---- chyby ----

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<Map<String, String>> domainError(DomainException e) {
    return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, String>> forbidden(AccessDeniedException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
  }
}
