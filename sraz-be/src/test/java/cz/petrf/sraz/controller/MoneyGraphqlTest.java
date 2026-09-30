package cz.petrf.sraz.controller;

import cz.petrf.sraz.TestcontainersConfiguration;
import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.db.repo.EventRepository;
import cz.petrf.sraz.db.repo.GroupMemberRepository;
import cz.petrf.sraz.db.repo.RoleRepository;
import cz.petrf.sraz.db.repo.UserRepository;
import cz.petrf.sraz.security.JwtService;
import cz.petrf.sraz.service.EmailService;
import cz.petrf.sraz.service.EventService;
import cz.petrf.sraz.service.GroupService;
import cz.petrf.sraz.service.RegistrationService;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * GraphQL peněz: IBAN skupiny, potvrzení účasti, uzavření vyúčtování, moje platby a bank.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class MoneyGraphqlTest {

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
  @Autowired
  GroupService groupService;
  @Autowired
  EventService eventService;
  @Autowired
  RegistrationService registrations;
  @Autowired
  EventRepository eventRepo;

  private User user(String name) {
    return userRepo.save(User.builder().publicName(name).email("money-" + UUID.randomUUID() + "@example.com")
        .roles(Set.of(roleRepo.findByName("ROLE_USER").orElseThrow())).build());
  }

  private ResultActions gql(User user, String query, Map<String, Object> variables) throws Exception {
    return mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + jwtService.generateToken(user))
        .content(objectMapper.writeValueAsString(Map.of("query", query, "variables", variables))));
  }

  @Test
  void organizerClosesEventAndPlayerSeesChargeWithIban() throws Exception {
    User org = user("Org");
    User player = user("Hráč");
    SportGroup group = groupService.create("Hokej", null, org);
    memberRepo.save(GroupMember.builder().group(group).user(player).email(player.getEmail())
        .memberType(MemberType.SUBSTITUTE).position(Position.PLAYER).status(MembershipStatus.ACTIVE).build());

    gql(org, "mutation($id: ID!) { groupUpdate(id: $id, input: {name: \"Hokej\", iban: \"CZ65 0800 0000 1920 0014 5399\", finesEnabled: true}) { id finesEnabled } }",
        Map.of("id", group.getId().toString()));

    Event event = eventService.create(group.getId(), EventService.EventData.builder().name("Hokej")
        .startsAt(OffsetDateTime.now().plusDays(2)).pricePerHour(new BigDecimal("1000")).regularFee(new BigDecimal("200")).build(), org);
    registrations.respond(event.getId(), org, RegistrationStatus.IN, null, RegistrationSource.WEB);
    registrations.respond(event.getId(), player, RegistrationStatus.IN, null, RegistrationSource.WEB);
    event.setStartsAt(OffsetDateTime.now().minusHours(2));
    eventRepo.flush();

    gql(org, "mutation($e: ID!) { eventClose(id: $e) { amount kind reason user { publicName } } }", Map.of("e", event.getId().toString()))
        .andExpect(jsonPath("$.data.eventClose", hasSize(2)))
        .andExpect(jsonPath("$.data.eventClose[0].amount").value(500.0));

    gql(player, "{ myCharges { amount iban paidAt event { name closedAt } } }", Map.of())
        .andExpect(jsonPath("$.data.myCharges", hasSize(1)))
        .andExpect(jsonPath("$.data.myCharges[0].iban").value("CZ6508000000192000145399"))
        .andExpect(jsonPath("$.data.myCharges[0].event.closedAt").isNotEmpty());

    gql(org, "mutation($g: ID!) { bankEntryAdd(groupId: $g, amount: -300, description: \"Pivo\") { amount } }",
        Map.of("g", group.getId().toString()))
        .andExpect(jsonPath("$.data.bankEntryAdd.amount").value(-300.0));

    gql(player, "query($id: ID!) { group(id: $id) { bankBalance bankEntries { kind amount } } }", Map.of("id", group.getId().toString()))
        .andExpect(jsonPath("$.data.group.bankBalance").value(-300.0))
        .andExpect(jsonPath("$.data.group.bankEntries", hasSize(2)));
  }
}
