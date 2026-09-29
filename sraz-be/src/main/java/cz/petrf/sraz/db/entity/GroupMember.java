package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Členství ve skupině. {@code user} je null, dokud pozvaný pozvánku nepřijme.
 */
@Entity
@Table(name = "group_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMember extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "group_id")
  private SportGroup group;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(nullable = false)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MemberType memberType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Position position;

  private boolean organizer;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MembershipStatus status;

  private String inviteToken;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invited_by")
  private User invitedBy;

  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime respondedAt;

  public boolean isActive() {
    return status==MembershipStatus.ACTIVE;
  }
}
