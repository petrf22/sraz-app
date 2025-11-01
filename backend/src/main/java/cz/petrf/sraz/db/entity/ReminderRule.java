package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reminder_rules", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reminder_label_id", nullable = false)
    private ReminderLabel reminderLabel;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Integer order;

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