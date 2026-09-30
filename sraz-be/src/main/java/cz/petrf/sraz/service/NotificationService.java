package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sestavení a odeslání e-mailů (pozvánka do skupiny, pozvánka na akci, změny přihlášky).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

  private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("EEEE d. M. yyyy 'v' H:mm", Locale.of("cs"));

  private final TemplateEngine templateEngine;
  private final EmailService emailService;

  @Value("${app.frontend-url:http://localhost:4200}")
  private String frontendUrl;
  @Value("${app.mail.from}")
  private String fromEmail;
  @Value("${app.time-zone:Europe/Prague}")
  private String timeZone;

  public void sendGroupInvite(GroupMember member, User invitedBy) {
    Context ctx = new Context();
    ctx.setVariable("groupName", member.getGroup().getName());
    ctx.setVariable("invitedBy", invitedBy.getPublicName());
    ctx.setVariable("memberType", member.getMemberType()==MemberType.REGULAR ? "stálý člen":"náhradník");
    ctx.setVariable("link", frontendUrl + "/pozvanka/" + member.getInviteToken());

    send(member.getEmail(), "Pozvánka do skupiny " + member.getGroup().getName(), "email/group-invite-email", ctx);
  }

  /**
   * Pozvánka na termín s tlačítky pro rychlou odpověď (odpověď se potvrdí až na webu).
   */
  public void sendEventInvitation(Invitation invitation, List<Team> teams, Position position, EventSummary summary) {
    Event event = invitation.getEvent();
    String base = frontendUrl + "/prihlaska/" + invitation.getToken();

    Context ctx = eventContext(event);
    ctx.setVariable("playerName", invitation.getUser().getPublicName());
    ctx.setVariable("summary", summary);
    ctx.setVariable("goalie", position==Position.GOALIE);
    ctx.setVariable("teamLinks", teams.stream()
        .map(t -> Map.of("name", t.getName(), "color", t.getColor()!=null ? t.getColor():"#1677ff",
            "url", base + "?volba=IN&tym=" + t.getId()))
        .toList());
    ctx.setVariable("inUrl", base + "?volba=IN");
    ctx.setVariable("outUrl", base + "?volba=OUT");
    ctx.setVariable("detailUrl", base);

    send(invitation.getUser().getEmail(), "Pozvánka: " + event.getName() + " – " + formatStart(event), "email/event-invitation-email", ctx);
  }

  /** Hráč postoupil z fronty na volné místo. */
  public void sendPromotedFromWaitlist(Registration registration) {
    Context ctx = eventContext(registration.getEvent());
    ctx.setVariable("playerName", registration.getUser().getPublicName());
    ctx.setVariable("teamName", registration.getTeam()!=null ? registration.getTeam().getName():null);
    ctx.setVariable("headline", "Uvolnilo se místo – jste přihlášen(a)");
    ctx.setVariable("message", "Někdo se odhlásil a vy jste postoupil(a) z fronty. Pokud nemůžete přijít, odhlaste se prosím co nejdřív.");

    send(registration.getUser().getEmail(), "Uvolnilo se místo: " + registration.getEvent().getName(), "email/event-notice-email", ctx);
  }

  /** Připomínka přihlášenému hráči před začátkem akce. */
  public void sendReminder(Registration registration) {
    Event event = registration.getEvent();
    Context ctx = eventContext(event);
    ctx.setVariable("playerName", registration.getUser().getPublicName());
    ctx.setVariable("teamName", registration.getTeam()!=null ? registration.getTeam().getName():null);
    ctx.setVariable("headline", "Připomínka: " + formatStart(event));
    ctx.setVariable("message", "Jste přihlášen(a). Pokud nemůžete přijít, dejte vědět organizátorovi.");

    send(registration.getUser().getEmail(), "Připomínka: " + event.getName() + " – " + formatStart(event), "email/event-notice-email", ctx);
  }

  /** Souhrn pro organizátora po uzávěrce přihlášek. */
  public void sendDeadlineSummary(Event event, User organizer, DeadlineSummary summary) {
    Context ctx = eventContext(event);
    ctx.setVariable("organizerName", organizer.getPublicName());
    ctx.setVariable("summary", summary);

    send(organizer.getEmail(), "Uzávěrka: " + event.getName() + " – " + formatStart(event), "email/deadline-summary-email", ctx);
  }

  /** Po vyúčtování akce: kolik účastník platí a jak (IBAN + variabilní symbol, QR v aplikaci). */
  public void sendCharge(Charge charge) {
    Event event = charge.getEvent();
    Context ctx = eventContext(event);
    ctx.setVariable("playerName", charge.getUser().getPublicName());
    ctx.setVariable("amount", charge.getAmount().stripTrailingZeros().toPlainString());
    ctx.setVariable("iban", event.getGroup().getIban());
    ctx.setVariable("variableSymbol", charge.getId());
    ctx.setVariable("paymentsUrl", frontendUrl + "/platby");
    ctx.setVariable("fineReason", switch (charge.getReason()) {
      case PLAYED -> null;
      case LATE_CANCEL -> "odhlásil(a) ses po uzávěrce";
      case NO_SHOW -> "na akci jsi nepřišel/nepřišla bez omluvy";
    });

    send(charge.getUser().getEmail(), (charge.getReason()==ChargeReason.PLAYED ? "Platba za ":"Pokuta za ") + event.getName() + " – " + formatStart(event), "email/charge-email", ctx);
  }

  public void sendEventCancelled(Event event, User recipient, String reason) {
    Context ctx = eventContext(event);
    ctx.setVariable("playerName", recipient.getPublicName());
    ctx.setVariable("teamName", null);
    ctx.setVariable("headline", "Akce je zrušena");
    ctx.setVariable("message", reason!=null ? reason:"Organizátor akci zrušil.");

    send(recipient.getEmail(), "Zrušeno: " + event.getName() + " – " + formatStart(event), "email/event-notice-email", ctx);
  }

  private Context eventContext(Event event) {
    Context ctx = new Context();
    ctx.setVariable("eventName", event.getName());
    ctx.setVariable("groupName", event.getGroup().getName());
    ctx.setVariable("startsAt", formatStart(event));
    ctx.setVariable("deadline", event.getSignupDeadline().atZoneSameInstant(ZoneId.of(timeZone)).format(DATE_TIME));
    ctx.setVariable("venue", event.getVenue());
    ctx.setVariable("note", event.getNote());
    ctx.setVariable("appUrl", frontendUrl + "/akce/" + event.getId());
    return ctx;
  }

  private String formatStart(Event event) {
    return event.getStartsAt().atZoneSameInstant(ZoneId.of(timeZone)).format(DATE_TIME);
  }

  private void send(String to, String subject, String template, Context ctx) {
    String html = templateEngine.process(template, ctx);
    emailService.sendHtmlEmail(fromEmail, to, subject, html);
  }

  /** Soupiska po uzávěrce: sloupce (tým / brankáři), fronta a stálí členové bez odpovědi. */
  public record DeadlineSummary(List<Column> columns, List<String> waitlist, List<String> noResponse, EventSummary counts) {
    public record Column(String title, List<String> names) {
    }
  }

  /** Obsazenost termínu pro text e-mailu. */
  public record EventSummary(int players, int maxPlayers, int goalies, int maxGoalies, int waitlist) {
  }
}
