package cz.petrf.sraz.controller;

import cz.petrf.sraz.TestcontainersConfiguration;
import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.db.repo.EventRepository;
import cz.petrf.sraz.db.repo.GroupMemberRepository;
import cz.petrf.sraz.db.repo.RoleRepository;
import cz.petrf.sraz.db.repo.UserRepository;
import cz.petrf.sraz.security.JwtService;
import cz.petrf.sraz.service.EmailService;
import cz.petrf.sraz.service.FioClient;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * GraphQL párování plateb z Fio API: token se nevrací, pohyby vidí jen organizátor.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class FioGraphqlTest {

  @MockitoBean
  EmailService emailService;
  @MockitoBean
  FioClient fio;

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
    return userRepo.save(User.builder().publicName(name).email("fio-" + UUID.randomUUID() + "@example.com")
        .roles(Set.of(roleRepo.findByName("ROLE_USER").orElseThrow())).build());
  }

  private ResultActions gql(User user, String query, Map<String, Object> variables) throws Exception {
    return mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + jwtService.generateToken(user))
        .content(objectMapper.writeValueAsString(Map.of("query", query, "variables", variables))));
  }

  @Test
  void organizerConnectsFioAndSeesTransactionsPlayerDoesNot() throws Exception {
    User org = user("Org");
    User player = user("Hráč");
    SportGroup group = groupService.create("Hokej", null, org);
    memberRepo.save(GroupMember.builder().group(group).user(player).email(player.getEmail())
        .memberType(MemberType.SUBSTITUTE).position(Position.PLAYER).status(MembershipStatus.ACTIVE).build());
    when(fio.transactions(eq("TOKEN"), any(), any())).thenReturn(List.of(new FioClient.Transaction(7, LocalDate.of(2026, 9, 28),
        new BigDecimal("300"), "CZK", "12345", "123/0800", "Plátce", "hokej")));
    Map<String, Object> vars = Map.of("g", group.getId().toString());

    gql(org, "mutation($g: ID!) { groupSetFioToken(groupId: $g, token: \"TOKEN\") { fetched created matched unmatched } }", vars)
        .andExpect(jsonPath("$.data.groupSetFioToken.created").value(1))
        .andExpect(jsonPath("$.data.groupSetFioToken.unmatched").value(1));

    gql(org, "query($g: ID!) { group(id: $g) { fioConnected fioLastSyncAt fioLastError bankTransactions(status: UNMATCHED) { id amount variableSymbol note charge { id } } } }", vars)
        .andExpect(jsonPath("$.data.group.fioConnected").value(true))
        .andExpect(jsonPath("$.data.group.fioLastSyncAt").isNotEmpty())
        .andExpect(jsonPath("$.data.group.bankTransactions", hasSize(1)))
        .andExpect(jsonPath("$.data.group.bankTransactions[0].note").value("Neznámý variabilní symbol"));

    gql(player, "query($g: ID!) { group(id: $g) { fioConnected bankTransactions { id } } }", vars)
        .andExpect(jsonPath("$.errors[0].extensions.code").value("FORBIDDEN"));
  }
}
