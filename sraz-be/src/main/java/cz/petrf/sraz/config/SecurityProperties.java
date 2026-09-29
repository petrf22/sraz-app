package cz.petrf.sraz.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Nastavení webové bezpečnosti ({@code app.security.*}).
 */
@Component
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class SecurityProperties {

  /** Povolené originy pro CORS – v produkci je web i API na stejném originu (Caddy), CORS se tam neuplatní. */
  private List<String> allowedOrigins = List.of("http://localhost:4200", "http://127.0.0.1:4200");

  /** Kolik reverzních proxy stojí před appkou (produkce: jen Caddy) – viz {@code ClientIpResolver}. */
  private int trustedProxyCount = 1;
}
