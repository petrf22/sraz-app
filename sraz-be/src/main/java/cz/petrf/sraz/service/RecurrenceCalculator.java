package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.Recurrence;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Výpočet dnů, kdy se v období koná akce.
 * <ul>
 *   <li>WEEKLY – vybrané dny v týdnu, každý N-tý týden (počítáno od týdne, ve kterém období začíná);
 *   „dvakrát do týdne" = dva dny.</li>
 *   <li>MONTHLY – n-tý výskyt vybraných dnů v měsíci (1–5, -1 = poslední), každý N-tý měsíc;
 *   „třikrát do měsíce" = např. 1., 2. a 3. pátek.</li>
 * </ul>
 */
public final class RecurrenceCalculator {

  private RecurrenceCalculator() {
  }

  public record Rule(LocalDate validFrom, LocalDate validTo, Recurrence recurrence, int interval,
                     Set<DayOfWeek> days, Set<Integer> monthWeeks) {
  }

  public static List<LocalDate> dates(Rule rule) {
    List<LocalDate> result = new ArrayList<>();
    LocalDate weekAnchor = rule.validFrom().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    YearMonth monthAnchor = YearMonth.from(rule.validFrom());

    for (LocalDate d = rule.validFrom(); !d.isAfter(rule.validTo()); d = d.plusDays(1)) {
      if (!rule.days().contains(d.getDayOfWeek())) {
        continue;
      }
      boolean matches = switch (rule.recurrence()) {
        case WEEKLY -> ChronoUnit.WEEKS.between(weekAnchor, d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
            % rule.interval()==0;
        case MONTHLY -> ChronoUnit.MONTHS.between(monthAnchor, YearMonth.from(d)) % rule.interval()==0
            && (rule.monthWeeks().contains((d.getDayOfMonth() - 1) / 7 + 1)
            || rule.monthWeeks().contains(-1) && d.plusDays(7).getMonth()!=d.getMonth());
      };
      if (matches) {
        result.add(d);
      }
    }
    return result;
  }
}
