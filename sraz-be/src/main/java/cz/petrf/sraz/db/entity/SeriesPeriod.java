package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

/**
 * Období série (např. 1. 9. – 31. 3.) s pravidlem opakování a nastavením termínů.
 */
@Entity
@Table(name = "series_periods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeriesPeriod extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "series_id")
  private EventSeries series;

  @Column(nullable = false)
  private LocalDate validFrom;

  @Column(nullable = false)
  private LocalDate validTo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Recurrence recurrence;

  /** Každý N-tý týden / měsíc. */
  private int intervalCount;

  /** Dny v týdnu, např. "MONDAY,THURSDAY". */
  @Column(nullable = false)
  private String daysOfWeek;

  /** Jen MONTHLY: pořadí výskytu dne v měsíci, např. "1,3"; -1 = poslední. */
  private String monthWeeks;

  @Column(nullable = false)
  private LocalTime startTime;

  private int durationMinutes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venue_id")
  private Venue venue;

  private int maxPlayersPerTeam;
  private int maxGoalies;
  private int deadlineHoursBefore;
  private int inviteRegularsHoursBefore;
  private int inviteSubstitutesHoursBefore;
  private Integer reminderHoursBefore;
  private String note;

  public List<DayOfWeek> days() {
    return Arrays.stream(daysOfWeek.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(DayOfWeek::valueOf).toList();
  }

  public List<Integer> weeksOfMonth() {
    if (monthWeeks==null || monthWeeks.isBlank()) {
      return List.of();
    }
    return Arrays.stream(monthWeeks.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(Integer::valueOf).toList();
  }
}
