package cz.petrf.sraz.controller;

import cz.petrf.sraz.TestcontainersConfiguration;
import cz.petrf.sraz.db.entity.User;
import cz.petrf.sraz.db.repo.RoleRepository;
import cz.petrf.sraz.db.repo.UserRepository;
import cz.petrf.sraz.security.JwtService;
import cz.petrf.sraz.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * GraphQL opakovaných akcí: série, období, náhled a vygenerované termíny v kalendáři skupiny.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class SeriesGraphqlTest {

  @MockitoBean
  EmailService emailService;

  @Autowired
  MockMvc mvc;
  @Autowired
  ObjectMapper objectMapper;
  @Autowired
  JwtService jwtService;
  @Autowired
  UserRepository userRepo;
  @Autowired
  RoleRepository roleRepo;

  private ResultActions gql(User user, String query, Map<String, Object> variables) throws Exception {
    return mvc.perform(post("/graphql")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + jwtService.generateToken(user))
        .content(objectMapper.writeValueAsString(Map.of("query", query, "variables", variables))));
  }

  private String data(ResultActions r, String pointer) throws Exception {
    return objectMapper.readTree(r.andReturn().getResponse().getContentAsString()).at(pointer).asString();
  }

  @Test
  void organizerCreatesSeasonAndSeesGeneratedEvents() throws Exception {
    User org = userRepo.save(User.builder().publicName("Org").email("series-" + UUID.randomUUID() + "@example.com")
        .roles(Set.of(roleRepo.findByName("ROLE_USER").orElseThrow())).build());
    String groupId = data(gql(org, "mutation { groupCreate(input: {name: \"Hokej\"}) { id } }", Map.of()), "/data/groupCreate/id");
    String seriesId = data(gql(org, "mutation($g: ID!) { seriesCreate(groupId: $g, name: \"Večerní hokej\") { id } }",
        Map.of("g", groupId)), "/data/seriesCreate/id");

    LocalDate from = LocalDate.now().plusWeeks(2).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    Map<String, Object> input = Map.of(
        "validFrom", from.toString(), "validTo", from.plusWeeks(3).minusDays(1).toString(),
        "recurrence", "WEEKLY", "daysOfWeek", java.util.List.of("MONDAY", "THURSDAY"),
        "startTime", "20:30", "maxPlayersPerTeam", 6, "reminderHoursBefore", 3);

    gql(org, "query($i: PeriodInput!) { periodPreview(input: $i) }", Map.of("i", input))
        .andExpect(jsonPath("$.data.periodPreview", hasSize(6)));

    gql(org, "mutation($s: ID!, $i: PeriodInput!) { periodSave(seriesId: $s, input: $i) { created updated removed kept } }",
        Map.of("s", seriesId, "i", input))
        .andExpect(jsonPath("$.data.periodSave.created").value(6));

    gql(org, """
        query($id: ID!) {
          group(id: $id) {
            series { name periods { daysOfWeek startTime recurrence monthWeeks reminderHoursBefore } }
            events { name maxPlayersPerTeam detached reminderHoursBefore series { name } }
          }
        }""", Map.of("id", groupId))
        .andExpect(jsonPath("$.data.group.series[0].periods[0].daysOfWeek", contains("MONDAY", "THURSDAY")))
        .andExpect(jsonPath("$.data.group.series[0].periods[0].startTime").value("20:30"))
        .andExpect(jsonPath("$.data.group.events", hasSize(6)))
        .andExpect(jsonPath("$.data.group.events[0].series.name").value("Večerní hokej"))
        .andExpect(jsonPath("$.data.group.events[0].maxPlayersPerTeam").value(6))
        .andExpect(jsonPath("$.data.group.events[0].detached").value(false));
  }

  @Test
  void invalidTimeIsReportedAsBadRequest() throws Exception {
    User org = userRepo.save(User.builder().publicName("Org").email("series-" + UUID.randomUUID() + "@example.com")
        .roles(Set.of(roleRepo.findByName("ROLE_USER").orElseThrow())).build());
    Map<String, Object> input = Map.of("validFrom", "2030-01-01", "validTo", "2030-01-31", "recurrence", "WEEKLY",
        "daysOfWeek", java.util.List.of("FRIDAY"), "startTime", "25:99");

    gql(org, "query($i: PeriodInput!) { periodPreview(input: $i) }", Map.of("i", input))
        .andExpect(jsonPath("$.errors[0].extensions.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.errors[0].message", containsString("HH:mm")));
  }
}
