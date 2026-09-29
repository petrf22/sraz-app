package cz.petrf.sraz.controller;

import cz.petrf.sraz.controller.GraphqlInputs.PeriodInput;
import cz.petrf.sraz.db.entity.EventSeries;
import cz.petrf.sraz.db.entity.SeriesPeriod;
import cz.petrf.sraz.db.entity.SportGroup;
import cz.petrf.sraz.exception.DomainException;
import cz.petrf.sraz.security.CurrentUserService;
import cz.petrf.sraz.service.SeriesService;
import cz.petrf.sraz.service.SeriesService.PeriodData;
import cz.petrf.sraz.service.SeriesService.SyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * GraphQL: opakované akce – série, období a náhled termínů.
 */
@Controller
@RequiredArgsConstructor
public class SeriesGraphqlController {

  private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

  private final CurrentUserService currentUser;
  private final SeriesService seriesService;

  @SchemaMapping(typeName = "SportGroup")
  public List<EventSeries> series(SportGroup group) {
    return seriesService.groupSeries(group.getId(), currentUser.requireUser());
  }

  @SchemaMapping
  public List<SeriesPeriod> periods(EventSeries series) {
    return seriesService.periods(series.getId());
  }

  @SchemaMapping(typeName = "SeriesPeriod")
  public List<DayOfWeek> daysOfWeek(SeriesPeriod period) {
    return period.days();
  }

  @SchemaMapping(typeName = "SeriesPeriod")
  public List<Integer> monthWeeks(SeriesPeriod period) {
    return period.weeksOfMonth();
  }

  @SchemaMapping(typeName = "SeriesPeriod")
  public String startTime(SeriesPeriod period) {
    return period.getStartTime().format(HH_MM);
  }

  @QueryMapping
  public List<OffsetDateTime> periodPreview(@Argument PeriodInput input) {
    currentUser.requireUser();
    return seriesService.preview(toData(input));
  }

  @MutationMapping
  public EventSeries seriesCreate(@Argument Long groupId, @Argument String name) {
    return seriesService.createSeries(groupId, name, currentUser.requireUser());
  }

  @MutationMapping
  public EventSeries seriesRename(@Argument Long id, @Argument String name) {
    return seriesService.renameSeries(id, name, currentUser.requireUser());
  }

  @MutationMapping
  public SyncResult seriesDelete(@Argument Long id) {
    return seriesService.deleteSeries(id, currentUser.requireUser());
  }

  @MutationMapping
  public SyncResult periodSave(@Argument Long seriesId, @Argument Long id, @Argument PeriodInput input) {
    return seriesService.savePeriod(seriesId, id, toData(input), currentUser.requireUser());
  }

  @MutationMapping
  public SyncResult periodDelete(@Argument Long id) {
    return seriesService.deletePeriod(id, currentUser.requireUser());
  }

  private static PeriodData toData(PeriodInput in) {
    return PeriodData.builder()
        .validFrom(in.validFrom())
        .validTo(in.validTo())
        .recurrence(in.recurrence())
        .intervalCount(in.intervalCount())
        .daysOfWeek(in.daysOfWeek())
        .monthWeeks(in.monthWeeks())
        .startTime(parseTime(in.startTime()))
        .durationMinutes(in.durationMinutes())
        .venueId(in.venueId())
        .maxPlayersPerTeam(in.maxPlayersPerTeam())
        .maxGoalies(in.maxGoalies())
        .deadlineHoursBefore(in.deadlineHoursBefore())
        .inviteRegularsHoursBefore(in.inviteRegularsHoursBefore())
        .inviteSubstitutesHoursBefore(in.inviteSubstitutesHoursBefore())
        .reminderHoursBefore(in.reminderHoursBefore())
        .note(in.note())
        .build();
  }

  private static LocalTime parseTime(String value) {
    try {
      return LocalTime.parse(value, HH_MM);
    } catch (DateTimeParseException | NullPointerException e) {
      throw new DomainException("Čas začátku musí být ve tvaru HH:mm.");
    }
  }
}
