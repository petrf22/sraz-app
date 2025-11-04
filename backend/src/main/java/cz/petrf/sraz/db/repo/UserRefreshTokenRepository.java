package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.UserRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {

  /* hledání podle JWT-ID */
  Optional<UserRefreshToken> findByJti(UUID jti);

  /* všechny aktivní tokeny uživatele */
  List<UserRefreshToken> findByUserIdAndRevokedIsFalse(Long userId);

  /* počet aktivních tokenů uživatele */
  long countByUserIdAndRevokedIsFalse(Long userId);

  /* hromadná revokace všech tokenů uživatele */
  @Modifying
  @Query("UPDATE UserRefreshToken t SET t.revoked = true WHERE t.user.id = :userId")
  int revokeAllByUserId(@Param("userId") Long userId);

  @Modifying
  @Query("""
      UPDATE UserRefreshToken urt
        SET urt.revoked = true
      WHERE urt.user.id = (select urt2.id
                    from UserRefreshToken urt2
                    where urt2.jti = :jti)
      """)
  int revokeAllByJit(@Param("jit") UUID jti);

  /* smazání už expirovaných tokenů (pro scheduled úklid) */
  void deleteAllByExpBefore(Instant now);
}