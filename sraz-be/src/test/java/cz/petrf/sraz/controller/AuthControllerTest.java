package cz.petrf.sraz.controller;

import cz.petrf.sraz.TestcontainersConfiguration;
import cz.petrf.sraz.db.entity.User;
import cz.petrf.sraz.db.repo.RoleRepository;
import cz.petrf.sraz.db.repo.UserRepository;
import cz.petrf.sraz.service.EmailService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Přihlášení kódem z e-mailu: žádost o kód, ověření, obnovení přes refresh cookie, odhlášení.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerTest {

  private static final Pattern CODE = Pattern.compile("(\\d{6})");

  @MockitoBean
  EmailService emailService;

  @Autowired
  MockMvc mvc;
  @Autowired
  ObjectMapper objectMapper;
  @Autowired
  UserRepository userRepo;
  @Autowired
  RoleRepository roleRepo;

  private static String email() {
    return "otp-" + UUID.randomUUID() + "@example.com";
  }

  private User existingUser(String email, boolean blocked) {
    return userRepo.save(User.builder()
        .publicName("Hráč")
        .email(email)
        .blockedAt(blocked ? OffsetDateTime.now():null)
        .roles(Set.of(roleRepo.findByName("ROLE_USER").orElseThrow()))
        .build());
  }

  /** Vyžádá kód a vrátí [challengeUid, kód z odeslaného e-mailu]. */
  private String[] requestCode(String email) throws Exception {
    String body = mvc.perform(post("/api/auth/otp/request").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\": \"%s\"}".formatted(email)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.expiresInSec").value(600))
        .andReturn().getResponse().getContentAsString();

    ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
    verify(emailService).sendHtmlEmail(anyString(), eq(email), anyString(), html.capture());
    Matcher m = CODE.matcher(html.getValue().replaceAll("(?s)<style>.*?</style>", ""));
    assertThat(m.find()).isTrue();
    return new String[]{objectMapper.readTree(body).get("challengeUid").asString(), m.group(1)};
  }

  private MvcResult verifyCode(String challengeUid, String code, String email, Boolean terms) throws Exception {
    return mvc.perform(post("/api/auth/otp/verify").contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new AuthController.OtpVerifyRequest(UUID.fromString(challengeUid), code, email, terms))))
        .andReturn();
  }

  @Test
  void newUserLogsInWithCodeAndConsentAndGetsRefreshCookie() throws Exception {
    String email = email();
    String[] challenge = requestCode(email);

    MvcResult result = verifyCode(challenge[0], challenge[1], email, true);

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(body.get("accessToken").asString()).isNotBlank();
    assertThat(body.get("newUser").asBoolean()).isTrue();
    assertThat(body.get("expiresInSec").asLong()).isEqualTo(600);
    Cookie cookie = result.getResponse().getCookie("refresh_token");
    assertThat(cookie).isNotNull();
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(userRepo.findByEmail(email)).get().extracting(User::getTermsAcceptedAt).isNotNull();
  }

  @Test
  void newUserWithoutConsentIsRejected() throws Exception {
    String email = email();
    String[] challenge = requestCode(email);

    MvcResult result = verifyCode(challenge[0], challenge[1], email, false);

    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    assertThat(result.getResponse().getContentAsString()).contains("TERMS_ACCEPTANCE_REQUIRED");
    assertThat(userRepo.findByEmail(email)).isEmpty();
  }

  @Test
  void existingUserDoesNotNeedConsentAgain() throws Exception {
    String email = email();
    existingUser(email, false);
    String[] challenge = requestCode(email);

    MvcResult result = verifyCode(challenge[0], challenge[1], email, null);

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getResponse().getContentAsString()).contains("\"newUser\":false");
  }

  @Test
  void codeIsSingleUseAndWrongCodeIsRejected() throws Exception {
    String email = email();
    existingUser(email, false);
    String[] challenge = requestCode(email);
    String wrong = challenge[1].equals("111111") ? "222222":"111111";

    assertThat(verifyCode(challenge[0], wrong, email, null).getResponse().getContentAsString()).contains("INVALID_CHALLENGE");
    assertThat(verifyCode(challenge[0], challenge[1], email, null).getResponse().getStatus()).isEqualTo(200);
    assertThat(verifyCode(challenge[0], challenge[1], email, null).getResponse().getStatus()).isEqualTo(400);
  }

  @Test
  void codeIsLockedAfterMaxAttempts() throws Exception {
    String email = email();
    existingUser(email, false);
    String[] challenge = requestCode(email);
    String wrong = challenge[1].equals("111111") ? "222222":"111111";

    for (int i = 0; i < 5; i++) {
      verifyCode(challenge[0], wrong, email, null);
    }

    assertThat(verifyCode(challenge[0], challenge[1], email, null).getResponse().getStatus()).isEqualTo(400);
  }

  @Test
  void codeCannotBeUsedForAnotherEmail() throws Exception {
    String email = email();
    existingUser(email, false);
    String[] challenge = requestCode(email);

    assertThat(verifyCode(challenge[0], challenge[1], email(), true).getResponse().getStatus()).isEqualTo(400);
  }

  @Test
  void secondRequestWithinCooldownIsRateLimited() throws Exception {
    String email = email();
    requestCode(email);

    mvc.perform(post("/api/auth/otp/request").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\": \"%s\"}".formatted(email)))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
  }

  @Test
  void blockedUserCannotRequestCode() throws Exception {
    String email = email();
    existingUser(email, true);

    mvc.perform(post("/api/auth/otp/request").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\": \"%s\"}".formatted(email)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCOUNT_BLOCKED"));
  }

  @Test
  void refreshRotatesTokenAndReuseRevokesTheWholeLogin() throws Exception {
    String email = email();
    existingUser(email, false);
    String[] challenge = requestCode(email);
    Cookie first = verifyCode(challenge[0], challenge[1], email, null).getResponse().getCookie("refresh_token");

    MvcResult refreshed = mvc.perform(post("/api/auth/refresh").cookie(first))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andReturn();
    Cookie second = refreshed.getResponse().getCookie("refresh_token");
    assertThat(second.getValue()).isNotEqualTo(first.getValue());

    // starý token použitý znovu = únik → zneplatní se i nový
    mvc.perform(post("/api/auth/refresh").cookie(first))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("SESSION_EXPIRED"));
    mvc.perform(post("/api/auth/refresh").cookie(second)).andExpect(status().isUnauthorized());
  }

  @Test
  void logoutRevokesRefreshTokenAndClearsCookie() throws Exception {
    String email = email();
    existingUser(email, false);
    String[] challenge = requestCode(email);
    Cookie cookie = verifyCode(challenge[0], challenge[1], email, null).getResponse().getCookie("refresh_token");

    mvc.perform(post("/api/auth/logout").cookie(cookie))
        .andExpect(status().isNoContent())
        .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

    mvc.perform(post("/api/auth/refresh").cookie(cookie)).andExpect(status().isUnauthorized());
  }

  @Test
  void refreshWithoutCookieIsUnauthorized() throws Exception {
    mvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
  }
}
