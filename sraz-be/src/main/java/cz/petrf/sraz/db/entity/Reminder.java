package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;

@Entity
@Table(name = "reminders", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reminder implements Persistable<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private Owner owner;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(nullable = false)
  private Integer hours;

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