package cz.petrf.sraz.controller;

import cz.petrf.sraz.config.AuthProperties;
import cz.petrf.sraz.db.entity.Role;
import cz.petrf.sraz.db.entity.User;
import cz.petrf.sraz.exception.AuthException;
import cz.petrf.sraz.exception.DomainException;
import cz.petrf.sraz.security.JwtService;
import cz.petrf.sraz.security.OtpService;
import cz.petrf.sraz.security.RefreshTokenService;
import cz.petrf.sraz.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Přihlášení kódem z e-mailu. Access token (JWT) jde v těle odpovědi a klient ho drží jen v paměti;
 * refresh token je v httpOnly cookie – díky ní se uživatel při další návštěvě nemusí přihlašovat.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private static final String REFRESH_COOKIE_NAME = "refresh_token";
  private static final String REFRESH_COOKIE_PATH = "/api/auth";

  private final OtpService otpService;
  private final RefreshTokenService refreshTokenService;
  private final JwtService jwtService;
  private final UserService userService;
  private final AuthProperties properties;

  public record OtpRequest(String email) {
  }

  public record OtpVerifyRequest(UUID challengeUid, String code, String email, Boolean termsAccepted) {
  }

  public record TokenResponse(String accessToken, long expiresInSec, boolean newUser, String email, List<String> roles) {
  }

  @PostMapping("/otp/request")
  public OtpService.OtpRequestResult requestOtp(@RequestBody OtpRequest request, HttpServletRequest servletRequest) {
    return otpService.requestOtp(request.email(), servletRequest.getRemoteAddr());
  }

  @PostMapping("/otp/verify")
  public ResponseEntity<TokenResponse> verifyOtp(@RequestBody OtpVerifyRequest request, HttpServletRequest servletRequest) {
    OtpService.VerifiedUser verified = otpService.verifyOtp(request.challengeUid(), request.code(), request.email(), request.termsAccepted());
    RefreshTokenService.IssuedToken refresh = refreshTokenService.issueNewFamily(verified.user(), deviceLabel(servletRequest));
    log.info("verifyOtp :: přihlášen uživatel {} (nový: {})", verified.user().getId(), verified.newUser());
    return respondWithTokens(verified.user(), refresh.rawToken(), verified.newUser());
  }

  @PostMapping("/refresh")
  public ResponseEntity<TokenResponse> refresh(HttpServletRequest servletRequest) {
    String rawToken = readCookie(servletRequest).orElseThrow(AuthException::sessionExpired);
    RefreshTokenService.IssuedToken issued = refreshTokenService.rotate(rawToken, deviceLabel(servletRequest));
    return respondWithTokens(issued.entity().getUser(), issued.rawToken(), false);
  }

  /**
   * Odhlášení na tomto zařízení; s {@code deleteAccount=true} navíc smaže účet (GDPR).
   */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestParam(name = "deleteAccount", defaultValue = "false") boolean deleteAccount,
                                     HttpServletRequest servletRequest) {
    readCookie(servletRequest).ifPresent(rawToken -> {
      if (deleteAccount) {
        userService.deleteAccountByRefreshToken(rawToken);
      } else {
        refreshTokenService.revokeFamilyByRawToken(rawToken);
      }
    });
    return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
  }

  private ResponseEntity<TokenResponse> respondWithTokens(User user, String refreshToken, boolean newUser) {
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie(refreshToken, properties.getRefreshToken().getTtl()).toString())
        .body(new TokenResponse(jwtService.generateToken(user), jwtService.accessTokenTtlSec(), newUser,
            user.getEmail(), user.getRoles().stream().map(Role::getName).toList()));
  }

  private ResponseCookie cookie(String value, Duration maxAge) {
    return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
        .httpOnly(true)
        .secure(properties.isCookieSecure())
        .sameSite("Strict")
        .path(REFRESH_COOKIE_PATH)
        .maxAge(maxAge)
        .build();
  }

  private static Optional<String> readCookie(HttpServletRequest request) {
    return Optional.ofNullable(request.getCookies()).stream().flatMap(Arrays::stream)
        .filter(c -> REFRESH_COOKIE_NAME.equals(c.getName()))
        .map(Cookie::getValue)
        .filter(StringUtils::isNotBlank)
        .findFirst();
  }

  private static String deviceLabel(HttpServletRequest request) {
    return StringUtils.truncate(StringUtils.defaultString(request.getHeader(HttpHeaders.USER_AGENT), "unknown"), 60);
  }

  @ExceptionHandler(AuthException.class)
  public ResponseEntity<Map<String, String>> authError(AuthException e) {
    return ResponseEntity.status(e.getStatus()).body(Map.of("code", e.getCode(), "message", e.getMessage()));
  }

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<Map<String, String>> domainError(DomainException e) {
    return ResponseEntity.badRequest().body(Map.of("code", "VALIDATION_FAILED", "message", e.getMessage()));
  }
}
