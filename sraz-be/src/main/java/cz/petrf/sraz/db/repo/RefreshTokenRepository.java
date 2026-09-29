package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.RefreshToken;
import cz.petrf.sraz.db.entity.RevokeReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByTokenHash(byte[] tokenHash);

  @Modifying
  @Query("""
      update RefreshToken t set t.revokedAt = :now, t.revokeReason = :reason
      where t.familyUid = :familyUid and t.revokedAt is null""")
  int revokeAllByFamilyUid(UUID familyUid, RevokeReason reason, OffsetDateTime now);

  @Modifying
  @Query("""
      update RefreshToken t set t.revokedAt = :now, t.revokeReason = :reason
      where t.user.id = :userId and t.revokedAt is null""")
  int revokeAllByUserId(Long userId, RevokeReason reason, OffsetDateTime now);

  /** Úklid: prošlé tokeny (zneplatněné i nepoužité). */
  @Modifying
  @Query("delete from RefreshToken t where t.expiresAt < :before")
  int deleteAllExpiredBefore(OffsetDateTime before);
}
