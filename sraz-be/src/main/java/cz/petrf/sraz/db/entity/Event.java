package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Konkrétní termín akce.
 */
@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "group_id")
  private SportGroup group;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venue_id")
  private Venue venue;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime startsAt;

  private int durationMinutes;
  private int maxPlayersPerTeam;
  private int maxGoalies;

  /** Po uzávěrce se hráči nemohou sami přihlásit ani odhlásit – řídí to organizátor. */
  @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime signupDeadline;

  private int inviteRegularsHoursBefore;
  private int inviteSubstitutesHoursBefore;

  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime regularsInvitedAt;

  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime substitutesInvitedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EventStatus status;

  private String note;

  public boolean isSignupOpen(OffsetDateTime now) {
    return (status==EventStatus.PLANNED || status==EventStatus.OPEN) && now.isBefore(signupDeadline);
  }
}
