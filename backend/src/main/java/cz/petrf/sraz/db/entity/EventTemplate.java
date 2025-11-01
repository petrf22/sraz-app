package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetTime;

@Entity
@Table(name = "event_templates", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @Column(nullable = false, length = 50)
    private String name;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isPublic = false;

    @Column(nullable = false)
    private Integer periodTypeId;
    @Column(nullable = false)
    private Boolean periodDayOfWeek;
    @Column(nullable = false)
    private Boolean periodRepeat;
    @Column(nullable = false)
    private Boolean periodTimes;

    private LocalDate startDate;
    @Column(nullable = false)
    private OffsetTime startTime;

    @Column(nullable = false, length = 100)
    private String placeName;

    @Column(length = 200)
    private String placeLocation;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal defaultPrice;

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