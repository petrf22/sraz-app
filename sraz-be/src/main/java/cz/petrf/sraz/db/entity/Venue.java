package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "venues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venue extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "group_id")
  private SportGroup group;

  @Column(nullable = false)
  private String name;

  private String address;
  private String mapUrl;
  private BigDecimal latitude;
  private BigDecimal longitude;
}
