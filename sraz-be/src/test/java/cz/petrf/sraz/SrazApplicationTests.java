package cz.petrf.sraz;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.crypto.SecretKey;

@SpringBootTest
class SrazApplicationTests {

	@Test
	void contextLoads() {
	}

  @Test
  void genJwtKey() {
    SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512); // 512 bitů
    String base64 = Encoders.BASE64.encode(key.getEncoded());
    System.out.println("JWT key: " + base64);   // vypíše 88 znaků
  }

}
