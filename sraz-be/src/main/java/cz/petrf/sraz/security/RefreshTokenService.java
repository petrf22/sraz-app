package cz.petrf.sraz.security;

import cz.petrf.sraz.config.AuthProperties;
import cz.petrf.sraz.db.entity.RefreshToken;
import cz.petrf.sraz.db.entity.RevokeReason;
import cz.petrf.sraz.db.entity.User;
import cz.petrf.sraz.db.repo.RefreshTokenRepository;
import cz.petrf.sraz.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Rotující refresh tokeny. Každé obnovení vydá nový token; opakované použití už použitého
 * tokenu (mimo krátkou toleranci) znamená jeho únik – zneplatní se celá rodina.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

  private final RefreshTokenRepository tokenRepo;
  private final AuthProperties properties;
  private final Clock clock;
  private final SecureRandom secureRandom = new SecureRandom();

  public record IssuedToken(String rawToken, RefreshToken entity) {
  }

  @Transactional
  public IssuedToken issueNewFamily(User user, String deviceLabel) {
    return issue(user, deviceLabel, UUID.randomUUID(), null);
  }

  @Transactional(noRollbackFor = AuthException.class)
  public IssuedToken rotate(String rawToken, String deviceLabel) {
    RefreshToken current = tokenRepo.findByTokenHash(sha256(rawToken)).orElseThrow(AuthException::sessionExpired);
    OffsetDateTime now = OffsetDateTime.now(clock);

    if (current.getRevokedAt()!=null || current.getExpiresAt().isBefore(now)) {
      throw AuthException.sessionExpired();
    }
    if (current.getUser().getBlockedAt()!=null) {
      tokenRepo.revokeAllByUserId(current.getUser().getId(), RevokeReason.BLOCKED, now);
      throw AuthException.sessionExpired();
    }
    if (current.getUsedAt()!=null) {
      // tolerance se počítá vždy od prvního použití a neprodlužuje se
      if (!current.getUsedAt().plus(properties.getRefreshToken().getReuseGracePeriod()).isAfter(now)) {
        int revoked = tokenRepo.revokeAllByFamilyUid(current.getFamilyUid(), RevokeReason.REUSE_DETECTED, now);
        log.warn("rotate :: opakované použití refresh tokenu, zneplatněno {} tokenů (uživatel {})", revoked, current.getUser().getId());
        throw AuthException.sessionExpired();
      }
    } else {
      current.setUsedAt(now);
    }
    return issue(current.getUser(), deviceLabel, current.getFamilyUid(), current);
  }

  /** Odhlášení = zneplatnění přihlášení na tomto zařízení (celé rodiny). */
  @Transactional
  public void revokeFamilyByRawToken(String rawToken) {
    tokenRepo.findByTokenHash(sha256(rawToken)).ifPresent(t ->
        tokenRepo.revokeAllByFamilyUid(t.getFamilyUid(), RevokeReason.LOGOUT, OffsetDateTime.now(clock)));
  }

  @Scheduled(cron = "@daily")
  @Transactional
  public void cleanup() {
    int deleted = tokenRepo.deleteAllExpiredBefore(OffsetDateTime.now(clock).minusDays(30));
    log.info("cleanup :: smazáno {} prošlých refresh tokenů", deleted);
  }

  private IssuedToken issue(User user, String deviceLabel, UUID familyUid, RefreshToken parent) {
    Hibernate.initialize(user);
    String rawToken = generateRawToken();
    RefreshToken token = tokenRepo.save(RefreshToken.builder()
        .familyUid(familyUid)
        .tokenHash(sha256(rawToken))
        .user(user)
        .parent(parent)
        .deviceLabel(deviceLabel)
        .expiresAt(OffsetDateTime.now(clock).plus(properties.getRefreshToken().getTtl()))
        .build());
    return new IssuedToken(rawToken, token);
  }

  private String generateRawToken() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  static byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
