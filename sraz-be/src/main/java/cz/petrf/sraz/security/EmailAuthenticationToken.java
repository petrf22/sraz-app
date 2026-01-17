package cz.petrf.sraz.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.Collection;

public class EmailAuthenticationToken extends AbstractAuthenticationToken {

  private final String email;

  public EmailAuthenticationToken(String email) {
    super(AuthorityUtils.NO_AUTHORITIES);     // zatím žádné authorities
    this.email = email;
    setAuthenticated(false);     // před průchodem providerem
  }

  /* po úspěšné autentizaci */
  public EmailAuthenticationToken(String email,
                                  Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.email = email;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return "";
  }  // heslo nepotřebujeme

  @Override
  public Object getPrincipal() {
    return email;
  }

  public String getEmail() {
    return email;
  }
}