package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role implements Persistable<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 50)
  private String name;

  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime createdAt;

  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime updatedAt;

  @ManyToMany(mappedBy = "roles")
  @Builder.Default
  private Set<User> users = new HashSet<>();

  @PrePersist
  protected void onCreate() {
    createdAt = OffsetDateTime.now();
    updatedAt = OffsetDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }

  @Override
  public boolean isNew() {
    return id==null;
  }
}