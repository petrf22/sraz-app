package cz.petrf.sraz.security;

import cz.petrf.sraz.config.AuthProperties;
import cz.petrf.sraz.db.entity.Role;
import cz.petrf.sraz.db.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * Krátkodobý access token (JWT, HS256). Dlouhodobé přihlášení drží refresh token ({@link RefreshTokenService}).
 */
@Service
@RequiredArgsConstructor
public class JwtService {

  private final AuthProperties properties;

  @Value("${jwt.secret}")
  private String secret;

  private SecretKey secretKey;

  public String generateToken(User dbUser) {
    long now = System.currentTimeMillis();
    return Jwts.builder()
        .subject(dbUser.getEmail())
        .claim("id", dbUser.getId())
        .claim("roles", dbUser.getRoles().stream().map(Role::getName).toList())
        .issuedAt(new Date(now))
        .expiration(new Date(now + properties.getAccessTokenTtl().toMillis()))
        .signWith(secretKey)
        .compact();
  }

  public long accessTokenTtlSec() {
    return properties.getAccessTokenTtl().toSeconds();
  }

  public Boolean validateToken(String token, UserDetails userDetails) {
    return extractUsername(token).equals(userDetails.getUsername()) && !extractExpiration(token).before(new Date());
  }

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    return claimsResolver.apply(extractAllClaims(token));
  }

  public Claims extractAllClaims(String token) {
    return jwtsParser().parseSignedClaims(token).getPayload();
  }

  private JwtParser jwtsParser() {
    return Jwts.parser().verifyWith(secretKey).build();
  }

  @PostConstruct
  public void postConstruct() {
    secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
  }
}
