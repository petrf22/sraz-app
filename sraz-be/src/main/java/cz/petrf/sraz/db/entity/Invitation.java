package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Pozvánka na termín – unikátní token pro dvojici osoba × akce, platný do začátku akce.
 */
@Entity
@Table(name = "invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invitation extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id")
  private Event event;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(nullable = false, unique = true)
  private String token;

  @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime expiresAt;

  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime sentAt;

  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime openedAt;

  public boolean isExpired(OffsetDateTime now) {
    return !now.isBefore(expiresAt);
  }
}
