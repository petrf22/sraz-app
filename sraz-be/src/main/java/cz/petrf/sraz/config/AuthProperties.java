package cz.petrf.sraz.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Nastavení přihlašování kódem z e-mailu ({@code app.auth.*}).
 */
@Component
@ConfigurationProperties(prefix = "app.auth")
@Getter
@Setter
public class AuthProperties {

  private final Otp otp = new Otp();
  private final RefreshToken refreshToken = new RefreshToken();
  /** Platnost access tokenu (JWT) – krátká, klient ho obnovuje přes refresh token. */
  private Duration accessTokenTtl = Duration.ofMinutes(10);
  /** Secure příznak refresh cookie – v produkci (HTTPS) true. */
  private boolean cookieSecure = true;

  @Getter
  @Setter
  public static class Otp {
    /** false = kód se jen zaloguje (vývoj bez SMTP). */
    private boolean mailEnabled = true;
    private Duration codeTtl = Duration.ofMinutes(10);
    private Duration resendCooldown = Duration.ofMinutes(1);
    private int maxAttempts = 5;
    private int emailHourlyLimit = 5;
    private int emailDailyLimit = 10;
    private int ipHourlyLimit = 20;
  }

  @Getter
  @Setter
  public static class RefreshToken {
    /** Jak dlouho si aplikace pamatuje přihlášení (každé použití prodlouží). */
    private Duration ttl = Duration.ofDays(90);
    /** Tolerance pro souběžné obnovení stejným tokenem (více záložek). */
    private Duration reuseGracePeriod = Duration.ofSeconds(30);
  }
}
