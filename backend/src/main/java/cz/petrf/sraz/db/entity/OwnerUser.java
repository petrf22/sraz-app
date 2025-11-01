package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "owner_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerUser {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private Owner owner;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Builder.Default
  @Column(nullable = false)
  private Boolean admin = false;

  @Builder.Default
  @Column(nullable = false)
  private Boolean active = true;

  @Column(nullable = false, updatable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();

  @Column(nullable = false)
  @Builder.Default
  private Instant updatedAt = Instant.now();

  /* automatická aktualizace updatedAt */
  @PreUpdate
  public void onUpdate() {
    updatedAt = Instant.now();
  }
}