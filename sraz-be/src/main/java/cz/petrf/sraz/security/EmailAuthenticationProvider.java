package cz.petrf.sraz.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailAuthenticationProvider implements AuthenticationProvider {

  private final UserDetailsService userDetailsService;   // váš servis, načte podle e-mailu

  @Override
  public Authentication authenticate(Authentication auth) throws AuthenticationException {
    String email = (String) auth.getPrincipal();

    UserDetails user = userDetailsService.loadUserByUsername(email);
    if (user==null) {
      throw new BadCredentialsException("Neznámý e-mail");
    }

    return new EmailAuthenticationToken(
        email,
        user.getAuthorities());
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return EmailAuthenticationToken.class.isAssignableFrom(authentication);
  }
}