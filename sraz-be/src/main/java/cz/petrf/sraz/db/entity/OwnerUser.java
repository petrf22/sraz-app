package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;

@Entity
@Table(name = "owner_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerUser implements Persistable<Long> {

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

  @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
  @Builder.Default
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
  @Builder.Default
  private OffsetDateTime updatedAt = OffsetDateTime.now();

  /* automatická aktualizace updatedAt */
  @PreUpdate
  public void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }

  @Override
  public boolean isNew() {
    return id==null;
  }
}