package cz.petrf.sraz.service;

import cz.petrf.sraz.db.repo.RefreshTokenRepository;
import cz.petrf.sraz.db.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepo;
  private final RefreshTokenRepository refreshTokenRepo;

  /**
   * Smazání účtu (GDPR). Členství, přihlášky a tokeny se smažou kaskádou, v auditu zůstane záznam bez autora.
   */
  @Transactional
  public void deleteAccountByRefreshToken(String rawToken) {
    refreshTokenRepo.findByTokenHash(sha256(rawToken)).ifPresent(token -> {
      log.info("deleteAccount :: mažu účet {}", token.getUser().getId());
      userRepo.delete(token.getUser());
    });
  }

  private static byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
