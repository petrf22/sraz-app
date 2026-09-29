package cz.petrf.sraz.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Výzva k přihlášení kódem z e-mailu. Kód je uložen jen jako hash, platí omezenou dobu,
 * má omezený počet pokusů a lze ho použít jen jednou.
 */
@Entity
@Table(name = "login_challenge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginChallenge {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private UUID challengeUid;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private String codeHash;

  @Column(nullable = false, columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime expiresAt;

  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime consumedAt;

  private short attempts;

  private short maxAttempts;

  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    if (challengeUid==null) {
      challengeUid = UUID.randomUUID();
    }
    createdAt = OffsetDateTime.now();
  }
}
