package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Částka, kterou účastník platí za termín. Variabilní symbol platby = id.
 */
@Entity
@Table(name = "charges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Charge extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id")
  private Event event;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ChargeKind kind;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime paidAt;

  @Enumerated(EnumType.STRING)
  private PaymentMethod paidMethod;

  public boolean isPaid() {
    return paidAt!=null;
  }
}
