package cz.petrf.sraz.service;

import cz.petrf.sraz.TestcontainersConfiguration;
import cz.petrf.sraz.db.entity.*;
import cz.petrf.sraz.db.repo.GroupMemberRepository;
import cz.petrf.sraz.db.repo.RoleRepository;
import cz.petrf.sraz.db.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Společný základ integračních testů služeb: reálný Postgres, e-maily jsou mockované,
 * každý test běží v transakci, která se na konci vrátí.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
abstract class ServiceTestSupport {

  @MockitoBean
  protected EmailService emailService;

  @Autowired
  protected UserRepository userRepo;
  @Autowired
  protected RoleRepository roleRepo;
  @Autowired
  protected GroupMemberRepository memberRepo;
  @Autowired
  protected GroupService groupService;
  @Autowired
  protected EventService eventService;

  protected User user(String name) {
    return userRepo.save(User.builder()
        .publicName(name)
        .email(name.toLowerCase() + "-" + UUID.randomUUID() + "@example.com")
        .roles(Set.of(roleRepo.findByName("ROLE_USER").orElseThrow()))
        .build());
  }

  /** Aktivní člen bez pozvánkového kolečka (to se testuje zvlášť). */
  protected GroupMember member(SportGroup group, User user, MemberType type, Position position) {
    return memberRepo.save(GroupMember.builder()
        .group(group)
        .user(user)
        .email(user.getEmail())
        .memberType(type)
        .position(position)
        .status(MembershipStatus.ACTIVE)
        .build());
  }

  protected Event event(SportGroup group, User organizer, int playersPerTeam, int goalies) {
    return eventService.create(group.getId(), EventService.EventData.builder()
        .name("Hokej")
        .startsAt(OffsetDateTime.now().plusDays(3))
        .maxPlayersPerTeam(playersPerTeam)
        .maxGoalies(goalies)
        .build(), organizer);
  }
}
