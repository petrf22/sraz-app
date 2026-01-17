package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetTime;

@Entity
@Table(name = "events", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private Owner owner;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_template_id", nullable = false)
  private EventTemplate eventTemplate;

  @Column(nullable = false, length = 255)
  private String name;

  @Builder.Default
  @Column(nullable = false)
  private Boolean active = true;

  @Builder.Default
  @Column(nullable = false)
  private Boolean isPublic = false;

  private LocalDate eventDate;
  @Column(nullable = false)
  private OffsetTime eventTime;

  private java.time.Instant deletedAt;

  @Builder.Default
  @Column(nullable = false, updatable = false)
  private java.time.Instant createdAt = java.time.Instant.now();

  @Builder.Default
  @Column(nullable = false)
  private java.time.Instant updatedAt = java.time.Instant.now();

  @PreUpdate
  public void onUpdate() {
    updatedAt = java.time.Instant.now();
  }
}