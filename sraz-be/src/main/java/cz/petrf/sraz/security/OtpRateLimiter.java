package cz.petrf.sraz.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import cz.petrf.sraz.config.AuthProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limity žádostí o přihlašovací kód (v paměti – po restartu se vynulují):
 * 1× za cooldown na e-mail, N za hodinu / den na e-mail a N za hodinu na IP.
 */
@Component
public class OtpRateLimiter {

  private final Cache<String, AtomicInteger> emailCooldown;
  private final Cache<String, AtomicInteger> emailHourly = cache(Duration.ofHours(1));
  private final Cache<String, AtomicInteger> emailDaily = cache(Duration.ofDays(1));
  private final Cache<String, AtomicInteger> ipHourly = cache(Duration.ofHours(1));
  private final AuthProperties.Otp props;

  public OtpRateLimiter(AuthProperties properties) {
    this.props = properties.getOtp();
    this.emailCooldown = cache(props.getResendCooldown());
  }

  public boolean tryAcquireForRequest(String email, String ipAddress) {
    return tryIncrement(emailCooldown, email, 1)
        && tryIncrement(emailHourly, email, props.getEmailHourlyLimit())
        && tryIncrement(emailDaily, email, props.getEmailDailyLimit())
        && (ipAddress==null || tryIncrement(ipHourly, ipAddress, props.getIpHourlyLimit()));
  }

  private static boolean tryIncrement(Cache<String, AtomicInteger> cache, String key, int max) {
    return cache.get(key, k -> new AtomicInteger()).incrementAndGet() <= max;
  }

  private static Cache<String, AtomicInteger> cache(Duration ttl) {
    return Caffeine.newBuilder().expireAfterWrite(ttl).build();
  }
}
