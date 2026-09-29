package cz.petrf.sraz.controller;

import cz.petrf.sraz.TestcontainersConfiguration;
import cz.petrf.sraz.db.entity.User;
import cz.petrf.sraz.db.repo.RoleRepository;
import cz.petrf.sraz.db.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthControllerTest {

  @Autowired
  MockMvc mvc;
  @Autowired
  UserRepository userRepo;
  @Autowired
  RoleRepository roleRepo;
  @Autowired
  PasswordEncoder encoder;

  private User user(String password, boolean blocked) {
    return userRepo.save(User.builder()
        .publicName("Hráč")
        .email("login-" + UUID.randomUUID() + "@example.com")
        .password(password.isEmpty() ? "":encoder.encode(password))
        .blockedAt(blocked ? OffsetDateTime.now():null)
        .roles(Set.of(roleRepo.findByName("ROLE_USER").orElseThrow()))
        .build());
  }

  private String body(User user, String password) {
    return """
        {"username": "%s", "password": "%s"}""".formatted(user.getEmail(), password);
  }

  @Test
  void passwordLoginReturnsAccessToken() throws Exception {
    User user = user("tajne-heslo", false);

    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(user, "tajne-heslo")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty());
  }

  @Test
  void wrongPasswordIsRejected() throws Exception {
    User user = user("tajne-heslo", false);

    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(user, "spatne")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void accountWithoutPasswordCannotLogInWithEmptyPassword() throws Exception {
    User user = user("", false);

    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(user, "")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void blockedUserCannotLogIn() throws Exception {
    User user = user("tajne-heslo", true);

    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(user, "tajne-heslo")))
        .andExpect(status().isUnauthorized());
  }
}
