package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "registrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Registration extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id")
  private Event event;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RegistrationStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Position position;

  /** U hráče tým, kde chce hrát (u WAITLIST preferovaný tým); u brankáře také tým. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id")
  private Team team;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RegistrationSource source;

  /** Pořadí ve frontě – kdy se hráč zařadil do WAITLIST. */
  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime queuedAt;

  /** Potvrzení účasti organizátorem po akci (null = zatím nepotvrzeno). */
  private Boolean attended;

  /** Odhlášen po uzávěrce – při zapnutých pokutách platí celý podíl, pokud není omluven. */
  private boolean lateCancel;

  /** Organizátor omluvil pozdní odhlášení / neúčast (bez pokuty). */
  private boolean excused;

  private int goals;
  private int assists;
}
