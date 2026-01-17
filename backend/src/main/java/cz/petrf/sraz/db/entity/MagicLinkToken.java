package cz.petrf.sraz.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "magic_link_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MagicLinkToken {
  @Id
  private String token;
  private String email;
  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime expiresAt;
  @Column(columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime usedAt;
  private boolean revoked;

  // Single-use pouze
  public boolean isValid() {
    return !revoked && usedAt==null && expiresAt.isAfter(OffsetDateTime.now());
  }
}