package cz.petrf.sraz.security;

import cz.petrf.sraz.db.entity.Role;
import cz.petrf.sraz.db.entity.User;
import cz.petrf.sraz.db.entity.UserRefreshToken;
import cz.petrf.sraz.db.repo.UserRefreshTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

  @Value("${jwt.secret}")
  private String secret;
  @Value("${jwt.expiration}")
  private Long expiration;
  private final Clock clock = Clock.systemUTC();
  private final UserRefreshTokenRepository userRefreshTokenRepository;
  private final SecretKey key = Jwts.SIG.HS512.key().build();

  public String generateToken(User dbUser) {
    Map<String, Object> claims = new HashMap<>();

    // Přidání rolí do claimů
    claims.put("roles", dbUser.getRoles().stream().map(Role::getName).toList());
    claims.put("id", dbUser.getId());

    return createToken(claims, dbUser.getEmail());
  }

  private String createToken(Map<String, Object> claims, String subject) {
    //byte[] keyBytes = Decoders.BASE64.decode(secret);
    // Key key = Keys.hmacShaKeyFor(keyBytes);

    return Jwts.builder()
        .subject(subject)
        .claims(claims)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(key)
        .compact();
  }

  public Boolean validateToken(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
  }

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  public Claims extractAllClaims(String token) {
    return jwtsParser().parseSignedClaims(token).getPayload();
  }

  private JwtParser jwtsParser() {
    return Jwts.parser().verifyWith(key).build();
  }

  private Boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  public String createAccess(User user) {
    return Jwts.builder()
        .subject(user.getEmail())
        .claim("id", user.getId())
        .issuedAt(Date.from(clock.instant()))
        .expiration(Date.from(clock.instant().plus(15, ChronoUnit.MINUTES)))
        .signWith(key)
        .compact();
  }

  public String createRefresh(User dbUser, UUID jti, String device, Duration maxAge) {
    Instant exp = clock.instant().plus(maxAge.toMillis(), ChronoUnit.MILLIS);

    save(dbUser, jti, device, exp);

    return Jwts.builder()
        .id(jti.toString())
        .issuedAt(Date.from(clock.instant()))
        .expiration(Date.from(exp))
        .signWith(key)
        .compact();
  }

  @Transactional
  public UserRefreshToken save(User user, UUID jti, String device, Instant exp) {
    return userRefreshTokenRepository.save(UserRefreshToken.builder()
        .user(user)
        .jti(jti)
        .device(device)
        .issuedAt(clock.instant())
        .exp(exp)
        .build());
  }

  /* validace: existuje a není revoked */
  public boolean isValid(UUID jti) {
    return userRefreshTokenRepository.findByJti(jti)
        .map(t -> !t.isRevoked() && t.getExp().isAfter(clock.instant()))
        .orElse(false);
  }

  /* změna hesla → zneplatni všechny refresh tokeny uživatele */
  @Transactional
  public void revokeAllForUser(Long userId) {
    userRefreshTokenRepository.revokeAllByUserId(userId);
  }

  /* denní úklid starých záznamů */
  @Scheduled(cron = "@daily")
  public void cleanup() {
    userRefreshTokenRepository.deleteAllByExpBefore(clock.instant());
  }

  public void revokeAllForUserByJti(UUID uuid) {
    int count = userRefreshTokenRepository.revokeAllByJit(uuid);
    log.info("Pro tuken JIT: {} bylo zneplatněno {} refresh tokenů.", uuid, count);
  }

  public String guessDevice(HttpServletRequest req) {
    String ua = req.getHeader("User-Agent");          // Mozilla/5.0 ...
    String ip = Optional.ofNullable(req.getHeader("X-Forwarded-For"))
        .map(f -> f.split(",")[0].trim())
        .orElse(req.getRemoteAddr());

    if (ua==null) return "unknown (" + ip + ")";

    /* velmi jednoduché rozpoznání */
    String type;
    ua = ua.toLowerCase();
    if (ua.contains("mobile")) type = "mobile";
    else if (ua.contains("tablet")) type = "tablet";
    else type = "desktop";

    return type + " / " + ip;
  }

  public Optional<User> findByJti(UUID uuid) {
    return userRefreshTokenRepository.findByJti(uuid).map(UserRefreshToken::getUser);
  }

}