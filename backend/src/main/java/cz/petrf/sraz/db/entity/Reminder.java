package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reminders", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "name"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Reminder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Integer hours;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private java.time.Instant createdAt = java.time.Instant.now();

    @Builder.Default
    @Column(nullable = false) private java.time.Instant updatedAt = java.time.Instant.now();

    @PreUpdate void onUpdate(){ updatedAt = java.time.Instant.now(); }
}