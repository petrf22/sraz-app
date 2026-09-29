package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Opakovaná akce (např. „Večerní hokej"), termíny generují její období.
 */
@Entity
@Table(name = "event_series")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSeries extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "group_id")
  private SportGroup group;

  @Column(nullable = false)
  private String name;
}
