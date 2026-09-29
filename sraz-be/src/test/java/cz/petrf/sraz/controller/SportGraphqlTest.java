package cz.petrf.sraz.controller;

import cz.petrf.sraz.TestcontainersConfiguration;
import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.db.repo.GroupMemberRepository;
import cz.petrf.sraz.db.repo.RoleRepository;
import cz.petrf.sraz.db.repo.UserRepository;
import cz.petrf.sraz.security.JwtService;
import cz.petrf.sraz.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * GraphQL API end-to-end přes HTTP: JWT, resolvery polí (líné načítání entit) a mapování chyb.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class SportGraphqlTest {

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
  @Autowired
  GroupMemberRepository memberRepo;

  User organizer;
  User player;
  User stranger;

  @BeforeEach
  void setUp() {
    organizer = user("Org");
    player = user("Hráč");
    stranger = user("Cizí");
  }

  private User user(String name) {
    return userRepo.save(User.builder()
        .publicName(name)
        .email("gql-" + UUID.randomUUID() + "@example.com")
        .roles(Set.of(roleRepo.findByName("ROLE_USER").orElseThrow()))
        .build());
  }

  private ResultActions gql(User user, String query, Map<String, Object> variables) throws Exception {
    var request = post("/graphql")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(Map.of("query", query, "variables", variables)));
    if (user!=null) {
      request.header("Authorization", "Bearer " + jwtService.generateToken(user));
    }
    return mvc.perform(request);
  }

  private String createGroup() throws Exception {
    String body = gql(organizer, """
        mutation { groupCreate(input: {name: "Večerní hokej"}) { id } }""", Map.of())
        .andReturn().getResponse().getContentAsString();
    return objectMapper.readTree(body).at("/data/groupCreate/id").asString();
  }

  private void addPlayer(String groupId) {
    memberRepo.save(GroupMember.builder()
        .group(memberRepo.findByGroupIdOrderByEmail(Long.valueOf(groupId)).getFirst().getGroup())
        .user(player)
        .email(player.getEmail())
        .memberType(MemberType.SUBSTITUTE)
        .position(Position.PLAYER)
        .status(MembershipStatus.ACTIVE)
        .build());
  }

  @Test
  void unauthenticatedRequestGetsUnauthenticatedCode() throws Exception {
    gql(null, "{ myGroups { id } }", Map.of())
        .andExpect(jsonPath("$.errors[0].extensions.code").value("UNAUTHENTICATED"));
  }

  @Test
  void organizerCreatesGroupWithDefaultTeamsAndSeesIt() throws Exception {
    String groupId = createGroup();

    gql(organizer, """
        query($id: ID!) { group(id: $id) { name amOrganizer teams { name } myMembership { organizer memberType } } }""",
        Map.of("id", groupId))
        .andExpect(jsonPath("$.data.group.name").value("Večerní hokej"))
        .andExpect(jsonPath("$.data.group.amOrganizer").value(true))
        .andExpect(jsonPath("$.data.group.teams[*].name", contains("Modří", "Červení")))
        .andExpect(jsonPath("$.data.group.myMembership.memberType").value("REGULAR"));

    gql(organizer, "{ myGroups { name } }", Map.of())
        .andExpect(jsonPath("$.data.myGroups[*].name", hasItem("Večerní hokej")));
  }

  @Test
  void strangerCannotReadGroup() throws Exception {
    String groupId = createGroup();

    gql(stranger, "query($id: ID!) { group(id: $id) { name } }", Map.of("id", groupId))
        .andExpect(jsonPath("$.errors[0].extensions.code").value("FORBIDDEN"));
  }

  @Test
  void memberEmailsAreHiddenFromOtherMembers() throws Exception {
    String groupId = createGroup();
    addPlayer(groupId);

    gql(player, "query($id: ID!) { group(id: $id) { members { email user { publicName } } } }", Map.of("id", groupId))
        .andExpect(jsonPath("$.data.group.members[?(@.user.publicName == 'Org')].email", contains(nullValue())))
        .andExpect(jsonPath("$.data.group.members[?(@.user.publicName == 'Hráč')].email", contains(player.getEmail())));
  }

  @Test
  void playerRegistersForEventAndRosterShowsIt() throws Exception {
    String groupId = createGroup();
    addPlayer(groupId);
    String startsAt = OffsetDateTime.now().plusDays(3).truncatedTo(ChronoUnit.SECONDS).toString();

    String body = gql(organizer, """
        mutation($g: ID!, $s: DateTime!) {
          eventCreate(groupId: $g, input: {name: "Hokej", startsAt: $s, maxPlayersPerTeam: 10}) { id status signupOpen }
        }""", Map.of("g", groupId, "s", startsAt))
        .andExpect(jsonPath("$.data.eventCreate.status").value("PLANNED"))
        .andExpect(jsonPath("$.data.eventCreate.signupOpen").value(true))
        .andReturn().getResponse().getContentAsString();
    String eventId = objectMapper.readTree(body).at("/data/eventCreate/id").asString();

    gql(player, """
        mutation($e: ID!) { registrationRespond(eventId: $e, status: IN) { status team { name } } }""",
        Map.of("e", eventId))
        .andExpect(jsonPath("$.data.registrationRespond.status").value("IN"))
        .andExpect(jsonPath("$.data.registrationRespond.team.name").value("Modří"));

    gql(player, """
        query($e: ID!) {
          event(id: $e) {
            group { name }
            summary { players maxPlayers }
            myRegistration { status }
            registrations { user { publicName } status source }
          }
        }""", Map.of("e", eventId))
        .andExpect(jsonPath("$.data.event.group.name").value("Večerní hokej"))
        .andExpect(jsonPath("$.data.event.summary.players").value(1))
        .andExpect(jsonPath("$.data.event.summary.maxPlayers").value(20))
        .andExpect(jsonPath("$.data.event.myRegistration.status").value("IN"))
        .andExpect(jsonPath("$.data.event.registrations[0].user.publicName").value("Hráč"))
        .andExpect(jsonPath("$.data.event.registrations[0].source").value("WEB"));
  }

  @Test
  void domainErrorIsReportedAsBadRequest() throws Exception {
    String groupId = createGroup();

    gql(organizer, """
        mutation($g: ID!) { memberInvite(groupId: $g, input: {email: "neplatny"}) { id } }""", Map.of("g", groupId))
        .andExpect(jsonPath("$.errors[0].extensions.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.errors[0].message", containsString("Neplatný e-mail")));
  }
}
