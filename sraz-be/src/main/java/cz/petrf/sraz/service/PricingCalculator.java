package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.ChargeKind;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rozpočítání ceny akce mezi účastníky.
 * <ul>
 *   <li>cena = cena za hodinu × délka</li>
 *   <li>podíl = cena / počet platících (bez brankářů), zaokrouhleno nahoru na 10 Kč</li>
 *   <li>stálý člen platí max(poplatek stálého člena, podíl), náhradník podíl, brankář nic</li>
 *   <li>vybráno − cena = přebytek (+) nebo schodek (−) do banku skupiny</li>
 * </ul>
 * Příklad (2800 Kč/h, poplatek 200): 10 lidí → všichni 280; 20 lidí (10 stálých + 10 náhradníků)
 * → stálí 200, náhradníci 140, do banku +600.
 */
public final class PricingCalculator {

  private static final BigDecimal ROUNDING = BigDecimal.TEN;

  private PricingCalculator() {
  }

  public record Participant(Long userId, ChargeKind kind) {
  }

  /** @param amounts částka pro každého účastníka (brankáři 0), v pořadí vstupu */
  public record Result(BigDecimal cost, BigDecimal share, Map<Long, BigDecimal> amounts, BigDecimal collected,
                       BigDecimal surplus) {
  }

  public static Result calculate(BigDecimal pricePerHour, int durationMinutes, BigDecimal regularFee, List<Participant> participants) {
    BigDecimal cost = pricePerHour.multiply(BigDecimal.valueOf(durationMinutes))
        .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    long payers = participants.stream().filter(p -> p.kind()!=ChargeKind.GOALIE).count();
    BigDecimal share = payers==0 ? BigDecimal.ZERO
        : cost.divide(BigDecimal.valueOf(payers).multiply(ROUNDING), 0, RoundingMode.CEILING).multiply(ROUNDING);

    Map<Long, BigDecimal> amounts = new LinkedHashMap<>();
    BigDecimal collected = BigDecimal.ZERO;
    for (Participant p : participants) {
      BigDecimal amount = switch (p.kind()) {
        case GOALIE -> BigDecimal.ZERO;
        case SUBSTITUTE -> share;
        case REGULAR -> regularFee!=null ? share.max(regularFee):share;
      };
      amounts.put(p.userId(), amount);
      collected = collected.add(amount);
    }
    return new Result(cost, share, amounts, collected, collected.subtract(cost));
  }
}
