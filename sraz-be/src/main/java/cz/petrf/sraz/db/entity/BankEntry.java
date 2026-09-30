package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Pohyb v banku skupiny (kladný = příjem, záporný = výdaj).
 */
@Entity
@Table(name = "bank_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankEntry extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "group_id")
  private SportGroup group;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BankEntryKind kind;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(nullable = false)
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id")
  private Event event;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;
}
