package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.db.repo.EventRepository;
import cz.petrf.sraz.exception.DomainException;
import cz.petrf.sraz.exception.ForbiddenException;
import cz.petrf.sraz.service.SeriesService.PeriodData;
import cz.petrf.sraz.service.SeriesService.SyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeriesServiceTest extends ServiceTestSupport {

  private static final ZoneId PRAGUE = ZoneId.of("Europe/Prague");

  @Autowired
  SeriesService seriesService;
  @Autowired
  RegistrationService registrations;
  @Autowired
  EventRepository eventRepo;

  User organizer;
  SportGroup group;
  EventSeries series;
  /** Pondělí za dva týdny – všechny termíny testů leží v budoucnu. */
  LocalDate monday;

  @BeforeEach
  void setUp() {
    organizer = user("Org");
    group = groupService.create("Večerní hokej", null, organizer);
    series = seriesService.createSeries(group.getId(), "Večerní hokej", organizer);
    monday = LocalDate.now(PRAGUE).plusWeeks(2).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
  }

  private PeriodData.PeriodDataBuilder fridays(LocalDate from, LocalDate to) {
    return PeriodData.builder()
        .validFrom(from).validTo(to)
        .recurrence(Recurrence.WEEKLY)
        .daysOfWeek(List.of(DayOfWeek.FRIDAY))
        .startTime(LocalTime.of(20, 0))
        .maxPlayersPerTeam(10)
        .reminderHoursBefore(3);
  }

  private List<Event> events() {
    return eventRepo.findBySeriesIdOrderByStartsAt(series.getId());
  }

  @Test
  void savingPeriodGeneratesAllFutureDatesWithPeriodSettings() {
    SyncResult result = seriesService.savePeriod(series.getId(), null, fridays(monday, monday.plusWeeks(4)).build(), organizer);

    assertThat(result).isEqualTo(new SyncResult(4, 0, 0, 0));
    Event first = events().getFirst();
    assertThat(first.getName()).isEqualTo("Večerní hokej");
    assertThat(first.getStartsAt().atZoneSameInstant(PRAGUE).toLocalDateTime())
        .isEqualTo(monday.plusDays(4).atTime(20, 0));
    assertThat(first.getSignupDeadline()).isEqualTo(first.getStartsAt().minusHours(24));
    assertThat(first.getReminderHoursBefore()).isEqualTo(3);
    assertThat(first.getStatus()).isEqualTo(EventStatus.PLANNED);
  }

  @Test
  void pastDatesAreNotGenerated() {
    LocalDate lastMonday = LocalDate.now(PRAGUE).minusWeeks(3).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

    seriesService.savePeriod(series.getId(), null, fridays(lastMonday, monday.plusWeeks(1)).build(), organizer);

    assertThat(events()).allMatch(e -> e.getStartsAt().isAfter(OffsetDateTime.now()));
  }

  @Test
  void changingPeriodUpdatesUntouchedEventsButKeepsThoseWithActivity() {
    SeriesPeriod period = savedPeriod(fridays(monday, monday.plusWeeks(3)).build());
    User player = user("Hráč");
    member(group, player, MemberType.REGULAR, Position.PLAYER);
    Event withRegistration = events().getFirst();
    registrations.respond(withRegistration.getId(), player, RegistrationStatus.IN, null, RegistrationSource.WEB);

    SyncResult result = seriesService.savePeriod(series.getId(), period.getId(),
        fridays(monday, monday.plusWeeks(3)).startTime(LocalTime.of(19, 0)).build(), organizer);

    assertThat(result).isEqualTo(new SyncResult(0, 2, 0, 1));
    assertThat(localTime(events().get(0))).isEqualTo(LocalTime.of(20, 0));   // má přihlášku – beze změny
    assertThat(localTime(events().get(1))).isEqualTo(LocalTime.of(19, 0));
  }

  @Test
  void shorterPeriodRemovesEventsWithoutActivityAndDetachesOthers() {
    SeriesPeriod period = savedPeriod(fridays(monday, monday.plusWeeks(3)).build());
    Event last = events().getLast();
    last.setRegularsInvitedAt(OffsetDateTime.now());   // pozvánky už odešly

    SyncResult result = seriesService.savePeriod(series.getId(), period.getId(), fridays(monday, monday.plusWeeks(1)).build(), organizer);

    assertThat(result).isEqualTo(new SyncResult(0, 1, 1, 1));
    assertThat(eventRepo.findById(last.getId())).get().extracting(Event::isDetached).isEqualTo(true);
  }

  @Test
  void cancelledDateIsNotRecreated() {
    SeriesPeriod period = savedPeriod(fridays(monday, monday.plusWeeks(2)).build());
    Event holiday = events().getFirst();
    eventService.cancel(holiday.getId(), "Svátek", organizer);

    SyncResult result = seriesService.savePeriod(series.getId(), period.getId(), fridays(monday, monday.plusWeeks(2)).build(), organizer);

    assertThat(result.created()).isZero();
    assertThat(events()).hasSize(2);
    assertThat(eventRepo.findById(holiday.getId())).get().extracting(Event::getStatus).isEqualTo(EventStatus.CANCELLED);
  }

  @Test
  void manuallyEditedEventIsNotOverwritten() {
    SeriesPeriod period = savedPeriod(fridays(monday, monday.plusWeeks(2)).build());
    Event edited = events().getFirst();
    eventService.update(edited.getId(), EventService.EventData.builder()
        .name("Hokej – derby").startsAt(edited.getStartsAt()).build(), organizer);

    seriesService.savePeriod(series.getId(), period.getId(),
        fridays(monday, monday.plusWeeks(2)).startTime(LocalTime.of(18, 0)).build(), organizer);

    Event after = eventRepo.findById(edited.getId()).orElseThrow();
    assertThat(after.isDetached()).isTrue();
    assertThat(after.getName()).isEqualTo("Hokej – derby");
    assertThat(localTime(after)).isEqualTo(LocalTime.of(20, 0));
  }

  @Test
  void overlappingPeriodsAreRejected() {
    savedPeriod(fridays(monday, monday.plusWeeks(4)).build());

    assertThatThrownBy(() -> seriesService.savePeriod(series.getId(), null, fridays(monday.plusWeeks(2), monday.plusWeeks(6)).build(), organizer))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("překrývá");
  }

  @Test
  void differentSettingsForDifferentPeriods() {
    // dvě období po dvou pátcích
    savedPeriod(fridays(monday, monday.plusDays(13)).build());
    savedPeriod(fridays(monday.plusDays(14), monday.plusDays(27)).maxPlayersPerTeam(8).startTime(LocalTime.of(21, 0)).build());

    assertThat(events()).extracting(Event::getMaxPlayersPerTeam).containsExactly(10, 10, 8, 8);
    assertThat(localTime(events().getLast())).isEqualTo(LocalTime.of(21, 0));
  }

  @Test
  void localTimeStaysTheSameAcrossDaylightSavingChange() {
    // poslední neděle v říjnu = přechod na zimní čas
    int year = LocalDate.now(PRAGUE).getYear() + 1;
    LocalDate dstEnd = LocalDate.of(year, 10, 31).with(TemporalAdjusters.lastInMonth(DayOfWeek.SUNDAY));

    // pátek před přechodem a pátek po něm
    savedPeriod(fridays(dstEnd.minusDays(2), dstEnd.plusDays(5)).build());

    List<Event> e = events();
    assertThat(e).hasSize(2);
    assertThat(e).allMatch(x -> localTime(x).equals(LocalTime.of(20, 0)));
    assertThat(e.get(0).getStartsAt().getOffset()).isEqualTo(ZoneOffset.ofHours(2));
    assertThat(e.get(1).getStartsAt().getOffset()).isEqualTo(ZoneOffset.ofHours(1));
  }

  @Test
  void deletingSeriesKeepsEventsWithActivityAsStandalone() {
    savedPeriod(fridays(monday, monday.plusWeeks(2)).build());
    Event active = events().getFirst();
    active.setSubstitutesInvitedAt(OffsetDateTime.now());

    SyncResult result = seriesService.deleteSeries(series.getId(), organizer);

    assertThat(result).isEqualTo(new SyncResult(0, 0, 1, 1));
    Event kept = eventRepo.findById(active.getId()).orElseThrow();
    assertThat(kept.getPeriod()).isNull();
  }

  @Test
  void previewIncludesAllDatesOfThePeriod() {
    List<OffsetDateTime> preview = seriesService.preview(fridays(monday, monday.plusWeeks(2)).build());

    assertThat(preview).hasSize(2);
    assertThat(events()).isEmpty();
  }

  @Test
  void monthlyRuleNeedsWeeksOfMonth() {
    assertThatThrownBy(() -> seriesService.preview(fridays(monday, monday.plusMonths(2)).recurrence(Recurrence.MONTHLY).build()))
        .isInstanceOf(DomainException.class);
  }

  @Test
  void onlyOrganizerManagesSeries() {
    User regular = user("Člen");
    member(group, regular, MemberType.REGULAR, Position.PLAYER);

    assertThatThrownBy(() -> seriesService.savePeriod(series.getId(), null, fridays(monday, monday.plusWeeks(1)).build(), regular))
        .isInstanceOf(ForbiddenException.class);
  }

  private SeriesPeriod savedPeriod(PeriodData data) {
    seriesService.savePeriod(series.getId(), null, data, organizer);
    return seriesService.periods(series.getId()).getLast();
  }

  private static LocalTime localTime(Event e) {
    return e.getStartsAt().atZoneSameInstant(PRAGUE).toLocalTime();
  }
}
