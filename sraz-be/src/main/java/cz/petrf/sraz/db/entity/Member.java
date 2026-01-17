package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;

@Entity
@Table(name = "members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"owner_id", "user_id"}),
    @UniqueConstraint(columnNames = {"owner_id", "public_name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member implements Persistable<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private Owner owner;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(nullable = false)
  private String publicName;

  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime deletedAt;

  @Builder.Default
  @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Builder.Default
  @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime updatedAt = OffsetDateTime.now();

  @PreUpdate
  public void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }

  @Override
  public boolean isNew() {
    return id==null;
  }
}