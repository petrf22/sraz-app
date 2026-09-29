package cz.petrf.sraz.controller;

import cz.petrf.sraz.TestcontainersConfiguration;
import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.db.repo.GroupMemberRepository;
import cz.petrf.sraz.db.repo.RoleRepository;
import cz.petrf.sraz.db.repo.UserRepository;
import cz.petrf.sraz.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Veřejné stránky z e-mailu: pozvánka na akci (token) a pozvánka do skupiny – bez přihlášení.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class PublicTokenControllerTest {

  @MockitoBean
  EmailService emailService;

  @Autowired
  MockMvc mvc;
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
  InvitationService invitationService;
  @Autowired
  MembershipService membershipService;

  User organizer;
  User player;
  SportGroup group;
  Event event;

  @BeforeEach
  void setUp() {
    organizer = user("Org");
    player = user("Hráč");
    group = groupService.create("Večerní hokej", null, organizer);
    memberRepo.save(GroupMember.builder().group(group).user(player).email(player.getEmail())
        .memberType(MemberType.REGULAR).position(Position.PLAYER).status(MembershipStatus.ACTIVE).build());
    event = eventService.create(group.getId(), EventService.EventData.builder()
        .name("Hokej").startsAt(OffsetDateTime.now().plusDays(3)).build(), organizer);
  }

  private User user(String name) {
    return userRepo.save(User.builder()
        .publicName(name)
        .email("public-" + UUID.randomUUID() + "@example.com")
        .password("")
        .roles(Set.of(roleRepo.findByName("ROLE_USER").orElseThrow()))
        .build());
  }

  @Test
  void invitationPageShowsEventAndLetsHolderSignUpWithoutLogin() throws Exception {
    String token = invitationService.getOrCreate(event, player).getToken();
    Long redTeamId = groupService.teams(group.getId()).get(1).getId();

    mvc.perform(get("/api/public/invitations/" + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventName").value("Hokej"))
        .andExpect(jsonPath("$.playerName").value("Hráč"))
        .andExpect(jsonPath("$.teams.length()").value(2))
        .andExpect(jsonPath("$.signupOpen").value(true))
        .andExpect(jsonPath("$.myStatus").doesNotExist());

    mvc.perform(post("/api/public/invitations/" + token).contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\": \"IN\", \"teamId\": %d}".formatted(redTeamId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.myStatus").value("IN"))
        .andExpect(jsonPath("$.myTeamId").value(redTeamId))
        .andExpect(jsonPath("$.roster[0].name").value("Hráč"));
  }

  @Test
  void unknownTokenReturnsBadRequestWithMessage() throws Exception {
    mvc.perform(get("/api/public/invitations/neexistuje"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Pozvánka neexistuje."));
  }

  @Test
  void groupInviteCanBeAcceptedByLink() throws Exception {
    GroupMember invite = membershipService.invite(group.getId(), "novy@example.com", MemberType.SUBSTITUTE, null, organizer);

    mvc.perform(get("/api/public/group-invites/" + invite.getInviteToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.groupName").value("Večerní hokej"))
        .andExpect(jsonPath("$.invitedBy").value("Org"))
        .andExpect(jsonPath("$.memberType").value("SUBSTITUTE"));

    mvc.perform(post("/api/public/group-invites/" + invite.getInviteToken()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"accept\": true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }
}
