package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;

@Entity
@Table(name = "events", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event implements Persistable<Long> {
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
  private Boolean publicVisible = false;

  private LocalDate eventDate;
  @Column(nullable = false)
  private OffsetTime eventTime;

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