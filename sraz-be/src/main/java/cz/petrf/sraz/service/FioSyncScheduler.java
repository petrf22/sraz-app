package cz.petrf.sraz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Pravidelně stahuje pohyby z Fio API u skupin s tokenem a páruje platby. Chyba jedné skupiny
 * (uloží se k ní jako fioLastError) nezastaví ostatní.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FioSyncScheduler {

  private final PaymentMatchingService paymentMatching;

  @Scheduled(fixedDelayString = "${app.fio.sync-interval:PT1H}", initialDelayString = "${app.fio.initial-delay:PT2M}")
  public void run() {
    for (Long groupId : paymentMatching.groupsWithToken()) {
      try {
        paymentMatching.sync(groupId);
      } catch (RuntimeException e) {
        log.warn("run :: synchronizace Fio skupiny {} selhala: {}", groupId, e.getMessage());
      }
    }
  }
}
