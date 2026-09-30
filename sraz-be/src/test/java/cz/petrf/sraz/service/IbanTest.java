package cz.petrf.sraz.service;

import cz.petrf.sraz.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IbanTest {

  @Test
  void normalizesValidIban() {
    assertThat(Iban.normalize(" cz65 0800 0000 1920 0014 5399 ")).isEqualTo("CZ6508000000192000145399");
  }

  @Test
  void emptyMeansNoAccount() {
    assertThat(Iban.normalize("  ")).isNull();
    assertThat(Iban.normalize(null)).isNull();
  }

  @Test
  void wrongChecksumIsRejected() {
    assertThatThrownBy(() -> Iban.normalize("CZ6508000000192000145398")).isInstanceOf(DomainException.class);
  }
}
