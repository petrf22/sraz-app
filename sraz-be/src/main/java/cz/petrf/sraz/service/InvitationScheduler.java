package cz.petrf.sraz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Pravidelné e-mailové úlohy: vlny pozvánek (stálí / náhradníci), připomínky přihlášeným
 * a souhrn organizátorům po uzávěrce. Každá úloha běží zvlášť, chyba jedné nezastaví ostatní.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvitationScheduler {

  private final InvitationService invitationService;
  private final ReminderService reminderService;

  @Scheduled(fixedDelayString = "${app.invitations.check-interval:PT5M}", initialDelayString = "${app.invitations.initial-delay:PT1M}")
  public void run() {
    run("pozvánky", invitationService::sendDueWaves);
    run("připomínky", reminderService::sendDueReminders);
    run("souhrny po uzávěrce", reminderService::sendDueDeadlineSummaries);
  }

  private static void run(String name, Runnable job) {
    try {
      job.run();
    } catch (RuntimeException e) {
      log.error("run :: chyba úlohy {}", name, e);
    }
  }
}
