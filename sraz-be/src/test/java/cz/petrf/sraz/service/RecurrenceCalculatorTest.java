package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.Recurrence;
import cz.petrf.sraz.service.RecurrenceCalculator.Rule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static java.time.DayOfWeek.*;
import static org.assertj.core.api.Assertions.assertThat;

class RecurrenceCalculatorTest {

  private static List<LocalDate> dates(String from, String to, Recurrence r, int interval, Set<java.time.DayOfWeek> days, Set<Integer> weeks) {
    return RecurrenceCalculator.dates(new Rule(LocalDate.parse(from), LocalDate.parse(to), r, interval, days, weeks));
  }

  @Test
  void weeklyOnFridaysForTheWholeHockeySeason() {
    List<LocalDate> d = dates("2026-09-01", "2027-03-31", Recurrence.WEEKLY, 1, Set.of(FRIDAY), Set.of());

    assertThat(d).first().isEqualTo(LocalDate.parse("2026-09-04"));
    assertThat(d).last().isEqualTo(LocalDate.parse("2027-03-26"));
    assertThat(d).hasSize(30).allMatch(x -> x.getDayOfWeek()==FRIDAY);
  }

  @Test
  void twiceAWeek() {
    assertThat(dates("2026-10-05", "2026-10-18", Recurrence.WEEKLY, 1, Set.of(MONDAY, THURSDAY), Set.of()))
        .containsExactly(LocalDate.parse("2026-10-05"), LocalDate.parse("2026-10-08"),
            LocalDate.parse("2026-10-12"), LocalDate.parse("2026-10-15"));
  }

  @Test
  void everySecondWeekCountsFromTheWeekThePeriodStarts() {
    // období začíná ve čtvrtek – týden 0 je týden od pondělí 28. 9.
    assertThat(dates("2026-10-01", "2026-10-31", Recurrence.WEEKLY, 2, Set.of(TUESDAY), Set.of()))
        .containsExactly(LocalDate.parse("2026-10-13"), LocalDate.parse("2026-10-27"));
  }

  @Test
  void threeTimesAMonthOnFirstSecondAndThirdFriday() {
    assertThat(dates("2026-10-01", "2026-10-31", Recurrence.MONTHLY, 1, Set.of(FRIDAY), Set.of(1, 2, 3)))
        .containsExactly(LocalDate.parse("2026-10-02"), LocalDate.parse("2026-10-09"), LocalDate.parse("2026-10-16"));
  }

  @Test
  void lastFridayOfTheMonth() {
    assertThat(dates("2026-10-01", "2026-12-31", Recurrence.MONTHLY, 1, Set.of(FRIDAY), Set.of(-1)))
        .containsExactly(LocalDate.parse("2026-10-30"), LocalDate.parse("2026-11-27"), LocalDate.parse("2026-12-25"));
  }

  @Test
  void monthlyEverySecondMonth() {
    assertThat(dates("2026-09-01", "2027-01-31", Recurrence.MONTHLY, 2, Set.of(MONDAY), Set.of(1)))
        .containsExactly(LocalDate.parse("2026-09-07"), LocalDate.parse("2026-11-02"), LocalDate.parse("2027-01-04"));
  }

  @Test
  void emptyWhenNoDayMatches() {
    assertThat(dates("2026-10-05", "2026-10-06", Recurrence.WEEKLY, 1, Set.of(FRIDAY), Set.of())).isEmpty();
  }
}
