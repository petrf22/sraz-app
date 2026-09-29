package cz.petrf.sraz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Pravidelně kontroluje, zda nenastal čas rozeslat pozvánky stálým členům nebo náhradníkům.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvitationScheduler {

  private final InvitationService invitationService;

  @Scheduled(fixedDelayString = "${app.invitations.check-interval:PT5M}", initialDelayString = "${app.invitations.initial-delay:PT1M}")
  public void sendDueInvitations() {
    try {
      invitationService.sendDueWaves();
    } catch (RuntimeException e) {
      log.error("sendDueInvitations :: chyba při rozesílání pozvánek", e);
    }
  }
}
