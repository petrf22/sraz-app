package cz.petrf.sraz.security;

import cz.petrf.sraz.config.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Klientská IP z {@code X-Forwarded-For} (převzato z kvalita-cena) – pro limity žádostí o kód.
 * <p>
 * Caddy ({@code sraz-fe/Caddyfile}) hlavičku přepisuje skutečnou spojovací IP; tenhle resolver
 * navíc bere hodnotu N-tou zprava podle {@code app.security.trusted-proxy-count} (výchozí 1), takže
 * položky, které si klient do hlavičky dopíše sám, zůstanou nalevo a limit na IP neobejdou.
 */
@Component
@RequiredArgsConstructor
public class ClientIpResolver {

  // Syntaktická kontrola IPv4/IPv6 literálu, žádné DNS
  private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}$");
  private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:]+:[0-9a-fA-F:]*$");

  private final SecurityProperties securityProperties;

  public String resolve(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor==null || forwardedFor.isBlank()) {
      return request.getRemoteAddr();
    }

    String[] parts = forwardedFor.split(",");
    int index = parts.length - Math.max(1, securityProperties.getTrustedProxyCount());
    if (index < 0) {
      // méně položek, než kolik proxy appka čeká – radši skutečná spojovací IP
      return request.getRemoteAddr();
    }

    String candidate = parts[index].trim();
    return isValidIp(candidate) ? candidate:request.getRemoteAddr();
  }

  private static boolean isValidIp(String value) {
    if (value.isEmpty() || value.length() > 45) {
      return false;
    }
    return IPV4.matcher(value).matches() || IPV6.matcher(value).matches();
  }
}
