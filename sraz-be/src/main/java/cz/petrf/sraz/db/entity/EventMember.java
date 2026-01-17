package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;


@Entity
@Table(name = "event_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventMember implements Persistable<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private Owner owner;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id", nullable = false)
  private Event event;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @Column(nullable = false, length = 40, unique = true)
  private String guid;

  @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime guidValidFrom;
  @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime guidValidTo;

  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime acceptedAt;
  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime declinedAt;

  @Builder.Default
  @Column(nullable = false)
  private Boolean attended = false;

  @Column(nullable = false, precision = 8, scale = 2)
  private BigDecimal paid;

  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime paidAt;

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