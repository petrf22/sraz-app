package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sport_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SportGroup extends BaseEntity {

  @Column(nullable = false)
  private String name;

  private String description;
}
