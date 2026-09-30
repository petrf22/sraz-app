package cz.petrf.sraz.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Párování plateb z Fio API (app.fio.*).
 */
@Component
@ConfigurationProperties(prefix = "app.fio")
@Getter
@Setter
public class FioProperties {

  /** Adresa REST API Fio banky. */
  private String baseUrl = "https://fioapi.fio.cz/v1/rest";

  /** Kolik dní zpět se pohyby stahují (duplicity řeší unikátní ID pohybu). */
  private int lookbackDays = 35;

  /** Klíč AES-256 pro šifrování tokenů v DB (base64, 32 bajtů) – v produkci FIO_TOKEN_KEY. */
  private String tokenKey;
}
