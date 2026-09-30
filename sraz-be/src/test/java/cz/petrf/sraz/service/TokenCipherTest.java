package cz.petrf.sraz.service;

import cz.petrf.sraz.config.FioProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenCipherTest {

  private static TokenCipher cipher(String key) {
    FioProperties properties = new FioProperties();
    properties.setTokenKey(key);
    return new TokenCipher(properties);
  }

  @Test
  void encryptsWithRandomIvAndDecryptsBack() {
    TokenCipher cipher = cipher("q3vN0wQ0cM7m3m0kH1m1hQ2p7rJkS0dXyq5b8uE1f4A=");

    String a = cipher.encrypt("fio-token-123");
    String b = cipher.encrypt("fio-token-123");

    assertThat(a).isNotEqualTo(b).doesNotContain("fio-token");
    assertThat(cipher.decrypt(a)).isEqualTo("fio-token-123");
  }

  @Test
  void otherKeyCannotDecrypt() {
    String encrypted = cipher("q3vN0wQ0cM7m3m0kH1m1hQ2p7rJkS0dXyq5b8uE1f4A=").encrypt("tajné");

    assertThatThrownBy(() -> cipher("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=").decrypt(encrypted))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rejectsMissingOrShortKey() {
    assertThatThrownBy(() -> cipher(null)).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> cipher("c2hvcnQ=")).isInstanceOf(IllegalStateException.class);
  }
}
