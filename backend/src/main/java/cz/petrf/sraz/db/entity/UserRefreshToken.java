package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_refresh_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false,
      foreignKey = @ForeignKey(name = "fk_refresh_user"))
  private User user;

  @Column(name = "jti", nullable = false, unique = true)
  private UUID jti;

  @Column(name = "device", length = 100)
  private String device;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  @Column(name = "exp", nullable = false)
  private Instant exp;

  @Builder.Default
  @Column(name = "revoked", nullable = false)
  private boolean revoked = false;
}