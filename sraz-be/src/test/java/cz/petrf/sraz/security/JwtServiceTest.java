package cz.petrf.sraz.security;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class JwtServiceTest {

  @Test
  void generateJwtKey() {
    byte[] key = Jwts.SIG.HS512.key().build().getEncoded();
    log.info("## JWT Configuration");
    log.info("jwt.secret={}", Base64.getEncoder().encodeToString(key));
    assertTrue(true);
  }
}