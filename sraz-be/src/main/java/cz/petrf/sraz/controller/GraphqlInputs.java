package cz.petrf.sraz.controller;

import cz.petrf.sraz.db.entity.MemberType;
import cz.petrf.sraz.db.entity.Position;

import java.time.OffsetDateTime;

/**
 * Vstupní typy GraphQL (odpovídají input typům v sport.graphqls).
 */
public final class GraphqlInputs {

  private GraphqlInputs() {
  }

  public record GroupInput(String name, String description) {
  }

  public record TeamInput(Long id, String name, String color, Integer sortOrder) {
  }

  public record VenueInput(Long id, String name, String address, String mapUrl, Double latitude, Double longitude) {
  }

  public record MemberInviteInput(String email, MemberType memberType, Position position) {
  }

  public record MemberUpdateInput(MemberType memberType, Position position, Boolean organizer) {
  }

  public record EventInput(String name, OffsetDateTime startsAt, Integer durationMinutes, Long venueId,
                           Integer maxPlayersPerTeam, Integer maxGoalies, OffsetDateTime signupDeadline,
                           Integer inviteRegularsHoursBefore, Integer inviteSubstitutesHoursBefore, String note) {
  }
}
