package cz.petrf.sraz.controller;

import cz.petrf.sraz.db.entity.User;
import cz.petrf.sraz.security.AppUser;
import cz.petrf.sraz.security.JwtService;
import cz.petrf.sraz.security.LoginRequest;
import cz.petrf.sraz.security.TokenDto;
import cz.petrf.sraz.service.UserDetailsServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final UserDetailsServiceImpl userDetailsService;
  private final JwtService jwtService;
  private final PasswordEncoder encoder;

  @Value("${app.refresh.token.max.age:90}")
  private int refreshTokenMaxAge;

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest req, HttpServletResponse resp) {
    try {
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              loginRequest.getUsername(),
              loginRequest.getPassword()
          )
      );

      SecurityContextHolder.getContext().setAuthentication(authentication);

      final AppUser appUser = userDetailsService.loadUserByUsername(loginRequest.getUsername());

      return createLoginResponseEntity(appUser.getDbUser(), req, resp);
    } catch (BadCredentialsException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }
  }

  @PostMapping("/refresh")
  public ResponseEntity<?> refresh(@CookieValue(name = "refresh", required = false) String refreshToken,
                                   HttpServletRequest req, HttpServletResponse resp) {

    /* 1. chybí cookie */
    if (refreshToken==null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    try {
      /* 2. parse */
      Claims claims = jwtService.extractAllClaims(refreshToken);
      Optional<UUID> jtiOpt = Optional.ofNullable(claims.getId()).map(UUID::fromString);

      /* 1. chybí JIT */
      if (jtiOpt.isEmpty()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }

      /* 3. blacklist */
      if (!jwtService.isValid(jtiOpt.get())) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }

      /* 4. najdi uživatele */
      Optional<User> userOpt = jwtService.findByJti(jtiOpt.get());

      if (userOpt.isEmpty()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }

      return createLoginResponseEntity(userOpt.get(), req, resp);
    } catch (JwtException | UsernameNotFoundException ex) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout(@CookieValue(name = "refresh", required = false) String refreshToken,
                                  HttpServletResponse resp) {

    if (refreshToken!=null) {
      try {
        String jti = jwtService.extractAllClaims(refreshToken).getId();
        jwtService.revokeAllForUserByJti(UUID.fromString(jti));
      } catch (JwtException ignored) {
      }
    }

    /* smaž cookie */
    ResponseCookie delete = ResponseCookie.from("refresh", "")
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .path("/refresh")
        .maxAge(0)
        .build();
    resp.addHeader(HttpHeaders.SET_COOKIE, delete.toString());

    return ResponseEntity.ok().build();
  }

  private ResponseEntity<TokenDto> createLoginResponseEntity(User dbUser, HttpServletRequest req, HttpServletResponse resp) {
    final String accessToken = jwtService.generateToken(dbUser);
    final String device = jwtService.guessDevice(req);
    final Duration maxAge = Duration.ofDays(refreshTokenMaxAge);
    final String refreshToken = jwtService.createRefresh(dbUser, UUID.randomUUID(), device, maxAge);

    ResponseCookie cookie = ResponseCookie.from("refresh", refreshToken)
        .httpOnly(true)
        .secure(true)          // true v produkci (HTTPS)
        .sameSite("Strict")
        .path("/refresh")
        .maxAge(maxAge)
        .build();

    resp.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    return ResponseEntity.ok(new TokenDto(accessToken));
  }
}

