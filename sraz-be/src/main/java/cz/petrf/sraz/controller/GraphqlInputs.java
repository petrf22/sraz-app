package cz.petrf.sraz.controller;

import cz.petrf.sraz.db.entity.MemberType;
import cz.petrf.sraz.db.entity.Position;
import cz.petrf.sraz.db.entity.Recurrence;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Vstupní typy GraphQL (odpovídají input typům v sport.graphqls).
 */
public final class GraphqlInputs {

  private GraphqlInputs() {
  }

  /** Částka z GraphQL Float (Kč). */
  public static java.math.BigDecimal money(Double value) {
    return value!=null ? java.math.BigDecimal.valueOf(value):null;
  }

  public record GroupInput(String name, String description, String iban) {
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
                           Integer inviteRegularsHoursBefore, Integer inviteSubstitutesHoursBefore,
                           Integer reminderHoursBefore, Double pricePerHour, Double regularFee, String note) {
  }

  public record PeriodInput(LocalDate validFrom, LocalDate validTo, Recurrence recurrence, Integer intervalCount,
                            List<DayOfWeek> daysOfWeek, List<Integer> monthWeeks, String startTime,
                            Integer durationMinutes, Long venueId, Integer maxPlayersPerTeam, Integer maxGoalies,
                            Integer deadlineHoursBefore, Integer inviteRegularsHoursBefore,
                            Integer inviteSubstitutesHoursBefore, Integer reminderHoursBefore,
                            Double pricePerHour, Double regularFee, String note) {
  }
}
