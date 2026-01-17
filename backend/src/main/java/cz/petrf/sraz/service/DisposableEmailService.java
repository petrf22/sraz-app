package cz.petrf.sraz.service;

import cz.petrf.sraz.config.DisposableEmailProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class DisposableEmailService {

  private final AtomicReference<Set<String>> blockedDomains = new AtomicReference<>(Collections.emptySet());

  private final DisposableEmailProperties properties;
  private final RestClient restClient;

  public DisposableEmailService(DisposableEmailProperties properties, RestClient.Builder restClientBuilder) {
    this.properties = properties;
    this.restClient = restClientBuilder.build();
  }

  @PostConstruct
  public void init() {
    updateBlocklist(); // načti hned při startu
  }

  @Scheduled(fixedRateString = "${app.disposable.update-interval-hours:24} * 60 * 60 * 1000", initialDelay = 60000)
  public void scheduledUpdate() {
    updateBlocklist();
  }

  private void updateBlocklist() {
    try {
      Set<String> newDomains = new HashSet<>();
      properties.getBlocklistUrls().forEach(url -> {
        try {
          log.info("Načítám blocklist z URL: {}", url);
          String content = restClient.get()
              .uri(url)
              .retrieve()
              .body(String.class);

          if (content!=null) {
            new BufferedReader(new StringReader(content))
                .lines()
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .filter(line -> !line.startsWith("#"))
                .map(StringUtils::lowerCase)
                .forEach(newDomains::add);
          }
        } catch (Exception ex) {
          log.warn("Nelze načíst blocklist z URL: {}", url, ex);
        }
      });

      blockedDomains.set(Collections.unmodifiableSet(newDomains));
      log.info("Disposable blocklist aktualizován: {} domén", newDomains.size());

    } catch (Exception ex) {
      log.error("Chyba při aktualizaci disposable blocklistu.", ex);
    }
  }

  public boolean isDisposable(String email) {
    if (StringUtils.isBlank(email) || !email.contains("@")) {
      return false;
    }

    String domain = StringUtils.substringAfter(email, '@').toLowerCase();
    log.info("isDisposable :: domain: {}", domain);

    do {
      if (getBlockedDomains().contains(domain)) {
        return true;
      }

      domain = StringUtils.substringAfter(domain, '.');
      log.info("isDisposable :: domain: {}", domain);
    } while (StringUtils.isNotEmpty(domain));

    return false;
  }

  public Set<String> getBlockedDomains() {
    return blockedDomains.get();
  }
}