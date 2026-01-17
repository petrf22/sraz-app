package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;

@Entity
@Table(name = "reminder_labels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderLabel implements Persistable<Long> {
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