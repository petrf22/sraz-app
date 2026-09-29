package cz.petrf.sraz.db.repo;

import cz.petrf.sraz.db.entity.LoginChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface LoginChallengeRepository extends JpaRepository<LoginChallenge, Long> {

  Optional<LoginChallenge> findByChallengeUidAndConsumedAtIsNull(UUID challengeUid);

  /** Pro jeden e-mail smí platit jen jedna výzva. */
  @Modifying
  @Query("update LoginChallenge c set c.consumedAt = :now where c.email = :email and c.consumedAt is null")
  int invalidateActiveChallenges(String email, OffsetDateTime now);

  /** Atomické zvýšení počtu pokusů – souběžné požadavky nepřekročí limit. */
  @Modifying
  @Query("update LoginChallenge c set c.attempts = c.attempts + 1 where c.id = :id and c.attempts < c.maxAttempts")
  int incrementAttempts(Long id);

  @Modifying
  @Query("delete from LoginChallenge c where c.expiresAt < :before")
  int deleteAllByExpiresAtBefore(OffsetDateTime before);
}
