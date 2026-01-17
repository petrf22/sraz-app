package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reminder_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private Owner owner;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reminder_label_id", nullable = false)
  private ReminderLabel reminderLabel;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id", nullable = false)
  private Event event;

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