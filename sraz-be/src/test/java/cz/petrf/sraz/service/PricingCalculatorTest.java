package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.ChargeKind;
import cz.petrf.sraz.service.PricingCalculator.Participant;
import cz.petrf.sraz.service.PricingCalculator.Result;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PricingCalculatorTest {

  private static final BigDecimal PRICE = new BigDecimal("2800");
  private static final BigDecimal FEE = new BigDecimal("200");

  private static List<Participant> people(int regulars, int substitutes, int goalies) {
    List<Participant> list = new ArrayList<>();
    long id = 1;
    for (int i = 0; i < regulars; i++) list.add(new Participant(id++, ChargeKind.REGULAR));
    for (int i = 0; i < substitutes; i++) list.add(new Participant(id++, ChargeKind.SUBSTITUTE));
    for (int i = 0; i < goalies; i++) list.add(new Participant(id++, ChargeKind.GOALIE));
    return list;
  }

  private static BigDecimal amountOf(Result r, long userId) {
    return r.amounts().get(userId);
  }

  @Test
  void tenPeopleEveryonePays280() {
    Result r = PricingCalculator.calculate(PRICE, 60, FEE, people(5, 5, 0));

    assertThat(r.share()).isEqualByComparingTo("280");
    assertThat(r.amounts().values()).allMatch(a -> a.compareTo(new BigDecimal("280"))==0);
    assertThat(r.surplus()).isEqualByComparingTo("0");
  }

  @Test
  void twentyPeopleRegularsPay200SubstitutesShareSurplusGoesToBank() {
    Result r = PricingCalculator.calculate(PRICE, 60, FEE, people(10, 10, 2));

    assertThat(r.share()).isEqualByComparingTo("140");
    assertThat(amountOf(r, 1)).isEqualByComparingTo("200");    // stálý
    assertThat(amountOf(r, 11)).isEqualByComparingTo("140");   // náhradník
    assertThat(amountOf(r, 21)).isEqualByComparingTo("0");     // brankář
    assertThat(r.collected()).isEqualByComparingTo("3400");
    assertThat(r.surplus()).isEqualByComparingTo("600");
  }

  @Test
  void shareIsRoundedUpToTenCrowns() {
    Result r = PricingCalculator.calculate(PRICE, 60, FEE, people(0, 13, 0));   // 2800 / 13 = 215,38

    assertThat(r.share()).isEqualByComparingTo("220");
    assertThat(r.surplus()).isEqualByComparingTo("60");
  }

  @Test
  void priceScalesWithDuration() {
    Result r = PricingCalculator.calculate(PRICE, 90, null, people(0, 10, 0));   // 4200 / 10

    assertThat(r.cost()).isEqualByComparingTo("4200");
    assertThat(r.share()).isEqualByComparingTo("420");
  }

  @Test
  void withoutRegularFeeRegularsPayTheShare() {
    Result r = PricingCalculator.calculate(PRICE, 60, null, people(20, 0, 0));

    assertThat(amountOf(r, 1)).isEqualByComparingTo("140");
  }

  @Test
  void onlyGoaliesMeansDeficitForTheBank() {
    Result r = PricingCalculator.calculate(PRICE, 60, FEE, people(0, 0, 2));

    assertThat(r.collected()).isEqualByComparingTo("0");
    assertThat(r.surplus()).isEqualByComparingTo("-2800");
  }
}
