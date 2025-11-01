// User.java
package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String publicName;

  private String firstName;
  private String lastName;

  @Column(nullable = false, unique = true)
  private String email;

  private Instant emailVerifiedAt;

  @Column(nullable = false)
  private String password;

  @Builder.Default
  @Column(nullable = false, updatable = false)
  private java.time.Instant createdAt = java.time.Instant.now();

  @Builder.Default
  @Column(nullable = false)
  private java.time.Instant updatedAt = java.time.Instant.now();

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "user_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id")
  )
  @Builder.Default
  private Set<Role> roles = new HashSet<>();

  @PreUpdate
  public void onUpdate() {
    updatedAt = java.time.Instant.now();
  }
}