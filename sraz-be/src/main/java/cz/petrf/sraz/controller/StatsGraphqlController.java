package cz.petrf.sraz.controller;

import cz.petrf.sraz.db.entity.Registration;
import cz.petrf.sraz.db.entity.SportGroup;
import cz.petrf.sraz.security.CurrentUserService;
import cz.petrf.sraz.service.StatsService;
import cz.petrf.sraz.service.StatsService.PlayerStats;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.List;

/**
 * GraphQL: statistiky hráčů a zápis gólů.
 */
@Controller
@RequiredArgsConstructor
public class StatsGraphqlController {

  private final CurrentUserService currentUser;
  private final StatsService statsService;

  @SchemaMapping(typeName = "SportGroup")
  public List<PlayerStats> stats(SportGroup group, @Argument LocalDate from, @Argument LocalDate to) {
    return statsService.groupStats(group.getId(), from, to, currentUser.requireUser());
  }

  @MutationMapping
  public Registration scoreSet(@Argument Long eventId, @Argument Long userId, @Argument int goals, @Argument int assists) {
    return statsService.setScore(eventId, userId, goals, assists, currentUser.requireUser());
  }
}
