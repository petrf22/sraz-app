package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.db.repo.*;
import cz.petrf.sraz.exception.DomainException;
import cz.petrf.sraz.exception.NotFoundException;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Opakované akce: série → období → vygenerované termíny.
 * <p>
 * Po uložení období se vygenerují všechny budoucí termíny (v kalendáři je vidět celá sezóna).
 * Změna období přegeneruje jen budoucí termíny bez aktivity (bez přihlášek a rozeslaných pozvánek);
 * termíny, které organizátor ručně upravil ({@code detached}) nebo zrušil, se nemění – zrušený
 * termín (např. svátek) se tak znovu nevytvoří.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SeriesService {

  /** Pojistka proti překlepu v datu (období na desítky let). */
  static final int MAX_PERIOD_DAYS = 731;

  private final EventSeriesRepository seriesRepo;
  private final SeriesPeriodRepository periodRepo;
  private final EventRepository eventRepo;
  private final RegistrationRepository registrationRepo;
  private final GroupService groupService;
  private final AccessService access;
  private final AuditService audit;
  private final Clock clock;

  @Value("${app.time-zone:Europe/Prague}")
  private String timeZone;

  /** Vstupní data období; null = výchozí hodnota (jako u jednorázové akce). */
  @Builder
  public record PeriodData(LocalDate validFrom, LocalDate validTo, Recurrence recurrence, Integer intervalCount,
                           List<DayOfWeek> daysOfWeek, List<Integer> monthWeeks, LocalTime startTime,
                           Integer durationMinutes, Long venueId, Integer maxPlayersPerTeam, Integer maxGoalies,
                           Integer deadlineHoursBefore, Integer inviteRegularsHoursBefore,
                           Integer inviteSubstitutesHoursBefore, Integer reminderHoursBefore,
                           BigDecimal pricePerHour, BigDecimal regularFee, String note) {
  }

  /** Výsledek synchronizace termínů s obdobím. */
  public record SyncResult(int created, int updated, int removed, int kept) {
  }

  // ---- série ----

  @Transactional(readOnly = true)
  public List<EventSeries> groupSeries(Long groupId, User user) {
    access.requireReader(groupId, user);
    return seriesRepo.findByGroupIdOrderByName(groupId);
  }

  @Transactional(readOnly = true)
  public List<SeriesPeriod> periods(Long seriesId) {
    return periodRepo.findBySeriesIdOrderByValidFrom(seriesId);
  }

  public EventSeries createSeries(Long groupId, String name, User user) {
    access.requireOrganizer(groupId, user);
    EventSeries series = seriesRepo.save(EventSeries.builder()
        .group(groupService.find(groupId))
        .name(GroupService.requireText(name, "Název série"))
        .build());
    audit.log(user, "SERIES_CREATE", "series", series.getId(), series.getName());
    return series;
  }

  /** Přejmenování se promítne i do budoucích termínů bez ruční úpravy. */
  public EventSeries renameSeries(Long seriesId, String name, User user) {
    EventSeries series = findSeries(seriesId);
    access.requireOrganizer(series.getGroup().getId(), user);
    series.setName(GroupService.requireText(name, "Název série"));
    OffsetDateTime now = OffsetDateTime.now(clock);
    eventRepo.findBySeriesIdOrderByStartsAt(seriesId).stream()
        .filter(e -> e.getStartsAt().isAfter(now) && !e.isDetached())
        .forEach(e -> e.setName(series.getName()));
    return series;
  }

  /** Smaže sérii: budoucí termíny bez aktivity zmizí, ostatní zůstanou jako samostatné akce. */
  public SyncResult deleteSeries(Long seriesId, User user) {
    EventSeries series = findSeries(seriesId);
    access.requireOrganizer(series.getGroup().getId(), user);
    SyncResult total = new SyncResult(0, 0, 0, 0);
    for (SeriesPeriod period : periodRepo.findBySeriesIdOrderByValidFrom(seriesId)) {
      total = add(total, removeGenerated(period));
      periodRepo.delete(period);
    }
    seriesRepo.delete(series);
    audit.log(user, "SERIES_DELETE", "series", seriesId, series.getName() + " " + total);
    return total;
  }

  // ---- období ----

  /** Náhled termínů období před uložením (včetně minulých – frontend je označí). */
  @Transactional(readOnly = true)
  public List<OffsetDateTime> preview(PeriodData data) {
    validate(data);
    return RecurrenceCalculator.dates(rule(data)).stream()
        .map(d -> ZonedDateTime.of(d, data.startTime(), zone()).toOffsetDateTime())
        .toList();
  }

  public SyncResult savePeriod(Long seriesId, Long periodId, PeriodData data, User user) {
    EventSeries series = findSeries(seriesId);
    Long groupId = series.getGroup().getId();
    access.requireOrganizer(groupId, user);
    validate(data);

    SeriesPeriod period = periodId==null
        ? SeriesPeriod.builder().series(series).build()
        : periodRepo.findById(periodId).filter(p -> p.getSeries().getId().equals(seriesId))
        .orElseThrow(() -> new NotFoundException("Období", periodId));

    periodRepo.findBySeriesIdOrderByValidFrom(seriesId).stream()
        .filter(p -> !p.getId().equals(period.getId()))
        .filter(p -> !p.getValidFrom().isAfter(data.validTo()) && !data.validFrom().isAfter(p.getValidTo()))
        .findFirst()
        .ifPresent(p -> {
          throw new DomainException("Období se překrývá s obdobím %s – %s.".formatted(p.getValidFrom(), p.getValidTo()));
        });

    apply(period, data, groupId);
    SeriesPeriod saved = periodRepo.save(period);
    SyncResult result = sync(saved);
    audit.log(user, "PERIOD_SAVE", "series_period", saved.getId(), result.toString());
    return result;
  }

  public SyncResult deletePeriod(Long periodId, User user) {
    SeriesPeriod period = periodRepo.findById(periodId).orElseThrow(() -> new NotFoundException("Období", periodId));
    access.requireOrganizer(period.getSeries().getGroup().getId(), user);
    SyncResult result = removeGenerated(period);
    periodRepo.delete(period);
    audit.log(user, "PERIOD_DELETE", "series_period", periodId, result.toString());
    return result;
  }

  /**
   * Srovná budoucí termíny období s pravidlem: chybějící vytvoří, změněné bez aktivity upraví,
   * přebytečné bez aktivity smaže. Termíny s aktivitou, ručně upravené a zrušené nechá být.
   */
  SyncResult sync(SeriesPeriod period) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    Map<LocalDate, OffsetDateTime> desired = new LinkedHashMap<>();
    for (LocalDate d : RecurrenceCalculator.dates(rule(period))) {
      OffsetDateTime start = ZonedDateTime.of(d, period.getStartTime(), zone()).toOffsetDateTime();
      if (start.isAfter(now)) {
        desired.put(d, start);
      }
    }

    int updated = 0, removed = 0, kept = 0;
    Set<LocalDate> taken = new HashSet<>();
    for (Event event : eventRepo.findByPeriodIdOrderByStartsAt(period.getId())) {
      if (!event.getStartsAt().isAfter(now)) {
        continue;   // minulost se nepřepisuje
      }
      LocalDate date = event.getStartsAt().atZoneSameInstant(zone()).toLocalDate();
      boolean frozen = event.isDetached() || event.getStatus()==EventStatus.CANCELLED || hasActivity(event);

      if (desired.containsKey(date)) {
        taken.add(date);
        if (frozen) {
          kept++;
        } else {
          fill(event, period, desired.get(date));
          updated++;
        }
      } else if (frozen) {
        // termín s aktivitou nemažeme – zůstane jako samostatná akce, rozhodne organizátor
        event.setDetached(true);
        kept++;
      } else {
        eventRepo.delete(event);
        removed++;
      }
    }

    int created = 0;
    for (Map.Entry<LocalDate, OffsetDateTime> entry : desired.entrySet()) {
      if (!taken.contains(entry.getKey())) {
        Event event = Event.builder()
            .group(period.getSeries().getGroup())
            .series(period.getSeries())
            .period(period)
            .status(EventStatus.PLANNED)
            .build();
        fill(event, period, entry.getValue());
        eventRepo.save(event);
        created++;
      }
    }
    SyncResult result = new SyncResult(created, updated, removed, kept);
    log.info("sync :: období {} → {}", period.getId(), result);
    return result;
  }

  /** Budoucí termíny období bez aktivity smaže, ostatní odpojí (zůstanou jako samostatné akce). */
  private SyncResult removeGenerated(SeriesPeriod period) {
    OffsetDateTime now = OffsetDateTime.now(clock);
    int removed = 0, kept = 0;
    for (Event event : eventRepo.findByPeriodIdOrderByStartsAt(period.getId())) {
      if (event.getStartsAt().isAfter(now) && !event.isDetached() && event.getStatus()!=EventStatus.CANCELLED && !hasActivity(event)) {
        eventRepo.delete(event);
        removed++;
      } else {
        event.setPeriod(null);
        event.setDetached(true);
        kept++;
      }
    }
    return new SyncResult(0, 0, removed, kept);
  }

  private boolean hasActivity(Event event) {
    return event.getRegularsInvitedAt()!=null || event.getSubstitutesInvitedAt()!=null
        || !registrationRepo.findByEventIdOrderByCreatedAt(event.getId()).isEmpty();
  }

  private void fill(Event event, SeriesPeriod p, OffsetDateTime start) {
    event.setName(p.getSeries().getName());
    event.setStartsAt(start);
    event.setDurationMinutes(p.getDurationMinutes());
    event.setVenue(p.getVenue());
    event.setMaxPlayersPerTeam(p.getMaxPlayersPerTeam());
    event.setMaxGoalies(p.getMaxGoalies());
    event.setSignupDeadline(start.minusHours(p.getDeadlineHoursBefore()));
    event.setInviteRegularsHoursBefore(p.getInviteRegularsHoursBefore());
    event.setInviteSubstitutesHoursBefore(p.getInviteSubstitutesHoursBefore());
    event.setReminderHoursBefore(p.getReminderHoursBefore());
    event.setPricePerHour(p.getPricePerHour());
    event.setRegularFee(p.getRegularFee());
    event.setNote(p.getNote());
  }

  private void apply(SeriesPeriod p, PeriodData d, Long groupId) {
    p.setValidFrom(d.validFrom());
    p.setValidTo(d.validTo());
    p.setRecurrence(d.recurrence());
    p.setIntervalCount(d.intervalCount()!=null ? d.intervalCount():1);
    p.setDaysOfWeek(d.daysOfWeek().stream().distinct().sorted().map(DayOfWeek::name).collect(Collectors.joining(",")));
    p.setMonthWeeks(d.recurrence()==Recurrence.MONTHLY
        ? d.monthWeeks().stream().distinct().sorted().map(String::valueOf).collect(Collectors.joining(",")):null);
    p.setStartTime(d.startTime());
    p.setDurationMinutes(orDefault(d.durationMinutes(), EventService.DEFAULT_DURATION_MINUTES));
    p.setVenue(d.venueId()!=null ? groupService.findVenue(groupId, d.venueId()):null);
    p.setMaxPlayersPerTeam(orDefault(d.maxPlayersPerTeam(), EventService.DEFAULT_PLAYERS_PER_TEAM));
    p.setMaxGoalies(orDefault(d.maxGoalies(), EventService.DEFAULT_GOALIES));
    p.setDeadlineHoursBefore(orDefault(d.deadlineHoursBefore(), EventService.DEFAULT_DEADLINE_HOURS));
    p.setInviteRegularsHoursBefore(orDefault(d.inviteRegularsHoursBefore(), EventService.DEFAULT_INVITE_REGULARS_HOURS));
    p.setInviteSubstitutesHoursBefore(orDefault(d.inviteSubstitutesHoursBefore(), EventService.DEFAULT_INVITE_SUBSTITUTES_HOURS));
    p.setReminderHoursBefore(d.reminderHoursBefore());
    p.setPricePerHour(EventService.money(d.pricePerHour(), "Cena za hodinu"));
    p.setRegularFee(EventService.money(d.regularFee(), "Poplatek stálého člena"));
    p.setNote(StringUtils.trimToNull(d.note()));
  }

  private void validate(PeriodData d) {
    if (d.validFrom()==null || d.validTo()==null || d.recurrence()==null || d.startTime()==null) {
      throw new DomainException("Období musí mít začátek, konec, typ opakování a čas začátku.");
    }
    if (d.validTo().isBefore(d.validFrom())) {
      throw new DomainException("Konec období je před jeho začátkem.");
    }
    if (ChronoUnit.DAYS.between(d.validFrom(), d.validTo()) > MAX_PERIOD_DAYS) {
      throw new DomainException("Období může trvat nejvýš dva roky.");
    }
    if (d.daysOfWeek()==null || d.daysOfWeek().isEmpty()) {
      throw new DomainException("Vyberte alespoň jeden den v týdnu.");
    }
    if (d.intervalCount()!=null && d.intervalCount() < 1) {
      throw new DomainException("Opakování musí být alespoň každý 1. týden / měsíc.");
    }
    if (d.recurrence()==Recurrence.MONTHLY) {
      if (d.monthWeeks()==null || d.monthWeeks().isEmpty()) {
        throw new DomainException("U měsíčního opakování vyberte, kolikátý den v měsíci (např. 1. a 3. pátek).");
      }
      if (d.monthWeeks().stream().anyMatch(w -> w!=-1 && (w < 1 || w > 5))) {
        throw new DomainException("Pořadí dne v měsíci musí být 1–5 nebo poslední.");
      }
    }
    for (Integer v : new Integer[]{d.durationMinutes(), d.maxPlayersPerTeam()}) {
      if (v!=null && v <= 0) {
        throw new DomainException("Délka akce a počet hráčů musí být kladná čísla.");
      }
    }
    for (Integer v : new Integer[]{d.maxGoalies(), d.deadlineHoursBefore(), d.inviteRegularsHoursBefore(),
        d.inviteSubstitutesHoursBefore(), d.reminderHoursBefore()}) {
      if (v!=null && v < 0) {
        throw new DomainException("Počty a předstihy v hodinách nesmí být záporné.");
      }
    }
  }

  private static RecurrenceCalculator.Rule rule(PeriodData d) {
    return new RecurrenceCalculator.Rule(d.validFrom(), d.validTo(), d.recurrence(),
        d.intervalCount()!=null ? d.intervalCount():1, EnumSet.copyOf(d.daysOfWeek()),
        d.monthWeeks()!=null ? Set.copyOf(d.monthWeeks()):Set.of());
  }

  private static RecurrenceCalculator.Rule rule(SeriesPeriod p) {
    return new RecurrenceCalculator.Rule(p.getValidFrom(), p.getValidTo(), p.getRecurrence(), p.getIntervalCount(),
        EnumSet.copyOf(p.days()), Set.copyOf(p.weeksOfMonth()));
  }

  private EventSeries findSeries(Long seriesId) {
    return seriesRepo.findById(seriesId).orElseThrow(() -> new NotFoundException("Série", seriesId));
  }

  private ZoneId zone() {
    return ZoneId.of(timeZone);
  }

  private static int orDefault(Integer value, int def) {
    return value!=null ? value:def;
  }

  private static SyncResult add(SyncResult a, SyncResult b) {
    return new SyncResult(a.created() + b.created(), a.updated() + b.updated(), a.removed() + b.removed(), a.kept() + b.kept());
  }
}
