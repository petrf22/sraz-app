package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Refresh token – v DB je jen SHA-256 náhodné hodnoty. Při každém použití se vydá nový
 * (rotace); tokeny jednoho přihlášení na jednom zařízení tvoří rodinu (familyUid).
 */
@Entity
@Table(name = "refresh_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private UUID familyUid;

  @Column(nullable = false, unique = true)
  private byte[] tokenHash;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private RefreshToken parent;

  private String deviceLabel;

  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime issuedAt;

  @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime expiresAt;

  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime usedAt;

  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime revokedAt;

  @Enumerated(EnumType.STRING)
  private RevokeReason revokeReason;

  @PrePersist
  protected void onCreate() {
    issuedAt = OffsetDateTime.now();
  }
}
