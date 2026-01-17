package cz.petrf.sraz.service;

import cz.petrf.sraz.config.DisposableEmailProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DisposableEmailServiceTest {

  private static void setBlockedDomains(DisposableEmailService service, Set<String> domains) throws Exception {
    Field field = DisposableEmailService.class.getDeclaredField("blockedDomains");
    field.setAccessible(true);
    @SuppressWarnings("unchecked")
    AtomicReference<Set<String>> ref = (AtomicReference<Set<String>>) field.get(service);
    ref.set(Set.copyOf(domains));
  }

  @Test
  void isDisposable() throws Exception {
    DisposableEmailProperties props = new DisposableEmailProperties();
    props.setBlocklistUrls(List.of("http://localhost/dummy.txt"));

    String baseUrl = "https://abc.go.com/v1";
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl);
    DisposableEmailService service = new DisposableEmailService(props, RestClient.builder().uriBuilderFactory(factory));

    setBlockedDomains(service, Set.of(
        "10minutemail.com",
        "mailinator.com",
        "sub.mailinator.com"
    ));

    assertThat(service.isDisposable("")).isFalse();
    assertThat(service.isDisposable("foo")).isFalse();
    assertThat(service.isDisposable("foo@")).isFalse();
    assertThat(service.isDisposable("@bar.cz")).isFalse();
    assertThat(service.isDisposable("jan@seznam.cz")).isFalse();

    assertThat(service.isDisposable("user@10minutemail.com")).isTrue();
    assertThat(service.isDisposable("UPPER@10MINUTEMAIL.COM")).isTrue();
    assertThat(service.isDisposable("aa@sub.mailinator.com")).isTrue();
    assertThat(service.isDisposable("x@mailinator.com")).isTrue();
  }
}