package cz.petrf.sraz.controller;

import cz.petrf.sraz.controller.GraphqlInputs.*;
import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.security.CurrentUserService;
import cz.petrf.sraz.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * GraphQL: skupiny, týmy, místa a členství.
 */
@Controller
@RequiredArgsConstructor
public class GroupGraphqlController {

  private final CurrentUserService currentUser;
  private final GroupService groupService;
  private final MembershipService membershipService;
  private final EventService eventService;
  private final AccessService access;

  @QueryMapping
  public List<SportGroup> myGroups() {
    return groupService.myGroups(currentUser.requireUser());
  }

  @QueryMapping
  public SportGroup group(@Argument Long id) {
    return groupService.get(id, currentUser.requireUser());
  }

  @QueryMapping
  public List<GroupMember> myGroupInvites() {
    return membershipService.pendingInvites(currentUser.requireUser());
  }

  // ---- pole typu SportGroup ----

  @SchemaMapping
  public List<Team> teams(SportGroup group) {
    return groupService.teams(group.getId());
  }

  @SchemaMapping
  public List<Venue> venues(SportGroup group) {
    return groupService.venues(group.getId(), currentUser.requireUser());
  }

  @SchemaMapping
  public List<GroupMember> members(SportGroup group) {
    return membershipService.members(group.getId(), currentUser.requireUser());
  }

  @SchemaMapping
  public GroupMember myMembership(SportGroup group) {
    return access.activeMembership(group.getId(), currentUser.requireUser()).orElse(null);
  }

  @SchemaMapping
  public boolean amOrganizer(SportGroup group) {
    return access.isOrganizer(group.getId(), currentUser.requireUser());
  }

  @SchemaMapping
  public List<Event> events(SportGroup group, @Argument OffsetDateTime from, @Argument OffsetDateTime to) {
    return eventService.groupEvents(group.getId(), from, to, currentUser.requireUser());
  }

  /** E-mail člena vidí jen organizátor a člen sám (ochrana osobních údajů). */
  @SchemaMapping(typeName = "GroupMember")
  public String email(GroupMember member) {
    User user = currentUser.requireUser();
    boolean self = member.getEmail().equalsIgnoreCase(user.getEmail());
    return self || access.isOrganizer(member.getGroup().getId(), user) ? member.getEmail():null;
  }

  // ---- mutace ----

  @MutationMapping
  public SportGroup groupCreate(@Argument GroupInput input) {
    return groupService.create(input.name(), input.description(), currentUser.requireUser());
  }

  @MutationMapping
  public SportGroup groupUpdate(@Argument Long id, @Argument GroupInput input) {
    return groupService.update(id, input.name(), input.description(), currentUser.requireUser());
  }

  @MutationMapping
  public Team teamSave(@Argument Long groupId, @Argument TeamInput input) {
    return groupService.saveTeam(groupId, input.id(), input.name(), input.color(), input.sortOrder(), currentUser.requireUser());
  }

  @MutationMapping
  public boolean teamDelete(@Argument Long id) {
    groupService.deleteTeam(id, currentUser.requireUser());
    return true;
  }

  @MutationMapping
  public Venue venueSave(@Argument Long groupId, @Argument VenueInput input) {
    return groupService.saveVenue(groupId, input.id(), input.name(), input.address(), input.mapUrl(),
        decimal(input.latitude()), decimal(input.longitude()), currentUser.requireUser());
  }

  @MutationMapping
  public boolean venueDelete(@Argument Long id) {
    groupService.deleteVenue(id, currentUser.requireUser());
    return true;
  }

  @MutationMapping
  public GroupMember memberInvite(@Argument Long groupId, @Argument MemberInviteInput input) {
    return membershipService.invite(groupId, input.email(), input.memberType(), input.position(), currentUser.requireUser());
  }

  @MutationMapping
  public GroupMember memberUpdate(@Argument Long id, @Argument MemberUpdateInput input) {
    return membershipService.update(id, input.memberType(), input.position(), input.organizer(), currentUser.requireUser());
  }

  @MutationMapping
  public GroupMember memberRemove(@Argument Long id) {
    return membershipService.remove(id, currentUser.requireUser());
  }

  @MutationMapping
  public GroupMember groupInviteRespond(@Argument Long memberId, @Argument boolean accept) {
    return membershipService.respondAsUser(memberId, accept, currentUser.requireUser());
  }

  private static BigDecimal decimal(Double value) {
    return value!=null ? BigDecimal.valueOf(value):null;
  }
}
