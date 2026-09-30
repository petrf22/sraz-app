package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.exception.DomainException;
import cz.petrf.sraz.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatsServiceTest extends ServiceTestSupport {

  @Autowired
  StatsService stats;
  @Autowired
  RegistrationService registrations;
  @Autowired
  AccountingService accounting;

  @Test
  void countsAttendanceGoalsAndMoneyOfPlayedEventsOnly() {
    User org = user("Org");
    SportGroup group = groupService.create("Hokej", null, org);
    User a = user("Adam"), b = user("Bára"), c = user("Cyril");
    for (User u : List.of(a, b, c)) {
      member(group, u, MemberType.SUBSTITUTE, Position.PLAYER);
    }

    Event played = eventService.create(group.getId(), EventService.EventData.builder().name("Hokej")
        .startsAt(OffsetDateTime.now().plusDays(2)).pricePerHour(new BigDecimal("600")).build(), org);
    registrations.respond(played.getId(), a, RegistrationStatus.IN, null, RegistrationSource.WEB);
    registrations.respond(played.getId(), b, RegistrationStatus.IN, null, RegistrationSource.WEB);
    registrations.respond(played.getId(), c, RegistrationStatus.OUT, null, RegistrationSource.WEB);
    played.setStartsAt(OffsetDateTime.now().minusHours(2));
    accounting.setAttendance(played.getId(), b.getId(), false, org);
    stats.setScore(played.getId(), a.getId(), 3, 1, org);
    accounting.close(played.getId(), org);

    // budoucí termín se nepočítá
    eventService.create(group.getId(), EventService.EventData.builder().name("Hokej").startsAt(OffsetDateTime.now().plusDays(5)).build(), org);

    List<StatsService.PlayerStats> result = stats.groupStats(group.getId(), null, null, org);

    StatsService.PlayerStats adam = result.stream().filter(s -> s.user().getId().equals(a.getId())).findFirst().orElseThrow();
    assertThat(adam.events()).isEqualTo(1);
    assertThat(adam.attended()).isEqualTo(1);
    assertThat(adam.goals()).isEqualTo(3);
    assertThat(adam.assists()).isEqualTo(1);
    assertThat(adam.charged()).isEqualByComparingTo("600");
    assertThat(adam.paid()).isEqualByComparingTo("0");
    assertThat(result.getFirst().user().getId()).isEqualTo(a.getId());   // řazeno podle účasti

    StatsService.PlayerStats bara = result.stream().filter(s -> s.user().getId().equals(b.getId())).findFirst().orElseThrow();
    assertThat(bara.noShow()).isEqualTo(1);
    assertThat(bara.attended()).isZero();

    StatsService.PlayerStats cyril = result.stream().filter(s -> s.user().getId().equals(c.getId())).findFirst().orElseThrow();
    assertThat(cyril.declined()).isEqualTo(1);

    StatsService.PlayerStats orgStats = result.stream().filter(s -> s.user().getId().equals(org.getId())).findFirst().orElseThrow();
    assertThat(orgStats.noAnswer()).isEqualTo(1);
  }

  @Test
  void scoreOnlyForRegisteredPlayerAndByOrganizer() {
    User org = user("Org");
    SportGroup group = groupService.create("Hokej", null, org);
    User a = user("Adam");
    member(group, a, MemberType.REGULAR, Position.PLAYER);
    Event e = eventService.create(group.getId(), EventService.EventData.builder().name("Hokej").startsAt(OffsetDateTime.now().plusDays(2)).build(), org);

    assertThatThrownBy(() -> stats.setScore(e.getId(), a.getId(), 1, 0, org)).isInstanceOf(DomainException.class);
    registrations.respond(e.getId(), a, RegistrationStatus.IN, null, RegistrationSource.WEB);
    assertThatThrownBy(() -> stats.setScore(e.getId(), a.getId(), 1, 0, a)).isInstanceOf(ForbiddenException.class);
    assertThatThrownBy(() -> stats.setScore(e.getId(), a.getId(), -1, 0, org)).isInstanceOf(DomainException.class);
  }
}
