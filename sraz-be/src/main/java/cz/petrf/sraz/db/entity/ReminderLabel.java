package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reminder_labels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderLabel {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private Owner owner;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reminder_id", nullable = false)
  private Reminder reminder;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "label_id", nullable = false)
  private Label label;

  private Integer limitToSend;
  private Integer hoursBefore;

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