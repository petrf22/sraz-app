package cz.petrf.sraz.security;

import cz.petrf.sraz.config.AuthProperties;
import cz.petrf.sraz.db.entity.LoginChallenge;
import cz.petrf.sraz.db.entity.User;
import cz.petrf.sraz.db.repo.LoginChallengeRepository;
import cz.petrf.sraz.db.repo.RoleRepository;
import cz.petrf.sraz.db.repo.UserRepository;
import cz.petrf.sraz.exception.AuthException;
import cz.petrf.sraz.exception.DomainException;
import cz.petrf.sraz.service.DisposableEmailService;
import cz.petrf.sraz.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Přihlášení bez hesla: na e-mail přijde šestimístný kód, který se zadá do aplikace.
 * Účet vzniká až po ověření kódu (se souhlasem se zpracováním údajů).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  private final LoginChallengeRepository challengeRepo;
  private final UserRepository userRepo;
  private final RoleRepository roleRepo;
  private final PasswordEncoder codeEncoder;
  private final OtpRateLimiter rateLimiter;
  private final EmailService emailService;
  private final DisposableEmailService disposableEmailService;
  private final TemplateEngine templateEngine;
  private final AuthProperties properties;
  private final Clock clock;
  private final SecureRandom secureRandom = new SecureRandom();

  @Value("${app.magic-link.mail.from:petr.franta@gmail.com}")
  private String fromEmail;

  public record OtpRequestResult(UUID challengeUid, long expiresInSec, long resendAfterSec) {
  }

  public record VerifiedUser(User user, boolean newUser) {
  }

  /**
   * Odešle kód. Chová se stejně pro existující i neexistující e-mail (nelze zjišťovat, kdo má účet).
   */
  @Transactional
  public OtpRequestResult requestOtp(String rawEmail, String ipAddress) {
    String email = normalize(rawEmail);
    if (!EMAIL.matcher(email).matches()) {
      throw new DomainException("Neplatný e-mail.");
    }
    if (disposableEmailService.isDisposable(email)) {
      throw new DomainException("Doména e-mailu %s není povolená.".formatted(email));
    }
    if (!rateLimiter.tryAcquireForRequest(email, ipAddress)) {
      throw AuthException.tooManyRequests();
    }
    if (userRepo.findByEmail(email).map(u -> u.getBlockedAt()!=null).orElse(false)) {
      throw AuthException.blocked();
    }

    OffsetDateTime now = OffsetDateTime.now(clock);
    AuthProperties.Otp otp = properties.getOtp();
    challengeRepo.invalidateActiveChallenges(email, now);

    String code = generateSixDigitCode();
    LoginChallenge challenge = challengeRepo.save(LoginChallenge.builder()
        .email(email)
        .codeHash(codeEncoder.encode(code))
        .expiresAt(now.plus(otp.getCodeTtl()))
        .maxAttempts((short) otp.getMaxAttempts())
        .build());

    if (otp.isMailEnabled()) {
      sendCode(email, code);
    } else {
      log.info("[DEV] Přihlašovací kód pro {} (výzva {}): {}", email, challenge.getChallengeUid(), code);
    }
    return new OtpRequestResult(challenge.getChallengeUid(), otp.getCodeTtl().toSeconds(), otp.getResendCooldown().toSeconds());
  }

  /**
   * Ověří kód. Všechny neúspěchy (neznámá / prošlá výzva, jiný e-mail, špatný kód, vyčerpané pokusy)
   * vracejí stejnou chybu.
   */
  @Transactional(noRollbackFor = AuthException.class)
  public VerifiedUser verifyOtp(UUID challengeUid, String code, String rawEmail, Boolean termsAccepted) {
    String email = normalize(rawEmail);
    OffsetDateTime now = OffsetDateTime.now(clock);
    LoginChallenge challenge = challengeRepo.findByChallengeUidAndConsumedAtIsNull(challengeUid)
        .orElseThrow(AuthException::invalidChallenge);

    if (!challenge.getEmail().equals(email) || challenge.getExpiresAt().isBefore(now)) {
      throw AuthException.invalidChallenge();
    }
    if (challengeRepo.incrementAttempts(challenge.getId())==0) {
      challenge.setConsumedAt(now);
      throw AuthException.invalidChallenge();
    }
    if (code==null || !codeEncoder.matches(code.strip(), challenge.getCodeHash())) {
      throw AuthException.invalidChallenge();
    }
    challenge.setConsumedAt(now);   // jednorázové použití

    User user = userRepo.findByEmail(email).orElse(null);
    boolean newUser = user==null;
    if (user!=null && user.getBlockedAt()!=null) {
      throw AuthException.blocked();
    }
    if (newUser) {
      if (!Boolean.TRUE.equals(termsAccepted)) {
        throw AuthException.termsRequired();
      }
      user = User.builder()
          .publicName(StringUtils.substringBefore(email, "@"))
          .email(email)
          .roles(Set.of(roleRepo.findByName("ROLE_USER").orElseThrow()))
          .termsAcceptedAt(now)
          .build();
    }
    if (user.getEmailVerifiedAt()==null) {
      user.setEmailVerifiedAt(now);
    }
    user.setLastLoginAt(now);
    return new VerifiedUser(userRepo.save(user), newUser);
  }

  /** Úklid prošlých výzev. */
  @Scheduled(cron = "@daily")
  @Transactional
  public void cleanup() {
    int deleted = challengeRepo.deleteAllByExpiresAtBefore(OffsetDateTime.now(clock).minusDays(1));
    log.info("cleanup :: smazáno {} prošlých přihlašovacích výzev", deleted);
  }

  private void sendCode(String email, String code) {
    Context ctx = new Context();
    ctx.setVariable("code", code);
    ctx.setVariable("minutes", properties.getOtp().getCodeTtl().toMinutes());
    emailService.sendHtmlEmail(fromEmail, email, "Přihlašovací kód – Sraz", templateEngine.process("email/otp-code-email", ctx));
  }

  /** Bez úvodní nuly kvůli čitelnosti: 100000–999999. */
  private String generateSixDigitCode() {
    return Integer.toString(100_000 + secureRandom.nextInt(900_000));
  }

  public static String normalize(String email) {
    return StringUtils.trimToEmpty(email).toLowerCase(Locale.ROOT);
  }
}
