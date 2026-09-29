package cz.petrf.sraz.security;

import cz.petrf.sraz.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

  private final ClientIpResolver resolver = new ClientIpResolver(new SecurityProperties());

  private static MockHttpServletRequest request(String forwardedFor) {
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setRemoteAddr("172.18.0.5");   // Caddy kontejner
    if (forwardedFor!=null) {
      req.addHeader("X-Forwarded-For", forwardedFor);
    }
    return req;
  }

  @Test
  void withoutHeaderUsesConnectionAddress() {
    assertThat(resolver.resolve(request(null))).isEqualTo("172.18.0.5");
  }

  @Test
  void takesTheEntryAddedByTheTrustedProxy() {
    assertThat(resolver.resolve(request("203.0.113.7"))).isEqualTo("203.0.113.7");
  }

  @Test
  void entriesForgedByTheClientAreIgnored() {
    // klient si poslal vlastní "1.2.3.4", Caddy připojil skutečnou IP vpravo
    assertThat(resolver.resolve(request("1.2.3.4, 203.0.113.7"))).isEqualTo("203.0.113.7");
  }

  @Test
  void invalidValueFallsBackToConnectionAddress() {
    assertThat(resolver.resolve(request("not-an-ip"))).isEqualTo("172.18.0.5");
  }

  @Test
  void ipv6IsAccepted() {
    assertThat(resolver.resolve(request("2001:db8::1"))).isEqualTo("2001:db8::1");
  }
}
