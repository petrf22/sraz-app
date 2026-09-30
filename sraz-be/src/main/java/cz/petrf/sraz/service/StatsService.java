package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.db.repo.ChargeRepository;
import cz.petrf.sraz.db.repo.EventRepository;
import cz.petrf.sraz.db.repo.GroupMemberRepository;
import cz.petrf.sraz.db.repo.RegistrationRepository;
import cz.petrf.sraz.exception.DomainException;
import cz.petrf.sraz.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/**
 * Statistiky hráčů skupiny za období: docházka, góly a asistence, peníze.
 * Počítají se jen odehrané termíny (začátek v minulosti, ne zrušené).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StatsService {

  private final EventRepository eventRepo;
  private final RegistrationRepository registrationRepo;
  private final ChargeRepository chargeRepo;
  private final GroupMemberRepository memberRepo;
  private final AccessService access;
  private final AuditService audit;
  private final Clock clock;

  @Value("${app.time-zone:Europe/Prague}")
  private String timeZone;

  /**
   * @param attended  přišel (přihlášen a účast nezrušena)
   * @param noShow    přihlášen, ale organizátor potvrdil, že nepřišel
   * @param declined  odhlásil se (OUT)
   * @param noAnswer  aktivní člen, který na termín nereagoval
   */
  public record PlayerStats(User user, MemberType memberType, Position position, int events, int attended,
                            int noShow, int declined, int noAnswer, int goals, int assists,
                            BigDecimal charged, BigDecimal paid) {
    public double attendanceRate() {
      return events==0 ? 0:(double) attended / events;
    }
  }

  /** Organizátor zapíše góly a asistence hráče na odehraném termínu. */
  public Registration setScore(Long eventId, Long userId, int goals, int assists, User organizer) {
    Event event = eventRepo.findById(eventId).orElseThrow(() -> new NotFoundException("Akce", eventId));
    access.requireOrganizer(event.getGroup().getId(), organizer);
    if (goals < 0 || assists < 0) {
      throw new DomainException("Góly a asistence nesmí být záporné.");
    }
    Registration reg = registrationRepo.findByEventIdAndUserId(eventId, userId)
        .filter(r -> r.getStatus()==RegistrationStatus.IN)
        .orElseThrow(() -> new DomainException("Góly jde zapsat jen přihlášenému hráči."));
    reg.setGoals(goals);
    reg.setAssists(assists);
    audit.log(organizer, "SCORE_SET", "registration", reg.getId(), "goals=%d assists=%d".formatted(goals, assists));
    return reg;
  }

  /** Statistiky aktivních členů za období (null = aktuální sezóna září–srpen). */
  @Transactional(readOnly = true)
  public List<PlayerStats> groupStats(Long groupId, LocalDate from, LocalDate to, User user) {
    access.requireReader(groupId, user);
    ZoneId zone = ZoneId.of(timeZone);
    LocalDate today = LocalDate.now(clock.withZone(zone));
    LocalDate seasonStart = LocalDate.of(today.getMonthValue() >= 9 ? today.getYear():today.getYear() - 1, 9, 1);
    OffsetDateTime start = (from!=null ? from:seasonStart).atStartOfDay(zone).toOffsetDateTime();
    OffsetDateTime end = (to!=null ? to.plusDays(1):seasonStart.plusYears(1)).atStartOfDay(zone).toOffsetDateTime();
    OffsetDateTime now = OffsetDateTime.now(clock);
    if (end.isAfter(now)) {
      end = now;
    }

    List<Event> played = end.isAfter(start)
        ? eventRepo.findByGroupIdAndStartsAtBetweenOrderByStartsAt(groupId, start, end).stream()
        .filter(e -> e.getStatus()!=EventStatus.CANCELLED).toList()
        :List.of();

    Map<Long, List<Registration>> regsByUser = new HashMap<>();
    Map<Long, BigDecimal[]> moneyByUser = new HashMap<>();
    for (Event e : played) {
      for (Registration r : registrationRepo.findByEventIdOrderByCreatedAt(e.getId())) {
        regsByUser.computeIfAbsent(r.getUser().getId(), k -> new ArrayList<>()).add(r);
      }
      for (Charge c : chargeRepo.findByEventIdOrderById(e.getId())) {
        BigDecimal[] m = moneyByUser.computeIfAbsent(c.getUser().getId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        m[0] = m[0].add(c.getAmount());
        if (c.isPaid()) {
          m[1] = m[1].add(c.getAmount());
        }
      }
    }

    List<PlayerStats> result = new ArrayList<>();
    for (GroupMember m : memberRepo.findByGroupIdOrderByEmail(groupId)) {
      if (!m.isActive() || m.getUser()==null) {
        continue;
      }
      List<Registration> regs = regsByUser.getOrDefault(m.getUser().getId(), List.of());
      int attended = 0, noShow = 0, declined = 0, goals = 0, assists = 0;
      for (Registration r : regs) {
        if (r.getStatus()==RegistrationStatus.IN) {
          if (Boolean.FALSE.equals(r.getAttended())) {
            noShow++;
          } else {
            attended++;
          }
        } else if (r.getStatus()==RegistrationStatus.OUT) {
          declined++;
        }
        goals += r.getGoals();
        assists += r.getAssists();
      }
      int noAnswer = Math.max(0, played.size() - regs.size());
      BigDecimal[] money = moneyByUser.getOrDefault(m.getUser().getId(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
      result.add(new PlayerStats(m.getUser(), m.getMemberType(), m.getPosition(), played.size(), attended, noShow,
          declined, noAnswer, goals, assists, money[0], money[1]));
    }
    result.sort(Comparator.comparingInt(PlayerStats::attended).reversed()
        .thenComparing(s -> s.user().getPublicName(), Comparator.nullsLast(Comparator.naturalOrder())));
    return result;
  }
}
