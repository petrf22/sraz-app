// Role.java
package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 50)
  private String name;

  @Builder.Default
  @Column(nullable = false, updatable = false)
  private java.time.Instant createdAt = java.time.Instant.now();

  @Builder.Default
  @Column(nullable = false)
  private java.time.Instant updatedAt = java.time.Instant.now();

  @ManyToMany(mappedBy = "roles")
  @Builder.Default
  private Set<User> users = new HashSet<>();

  @PreUpdate
  public void onUpdate() {
    updatedAt = java.time.Instant.now();
  }
}