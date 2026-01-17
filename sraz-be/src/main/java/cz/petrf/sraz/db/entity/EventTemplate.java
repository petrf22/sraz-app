package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;

@Entity
@Table(name = "event_templates", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventTemplate implements Persistable<Long> {
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
  private Boolean publicVisible = false;

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