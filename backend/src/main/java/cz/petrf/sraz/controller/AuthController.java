package cz.petrf.sraz.controller;

import cz.petrf.sraz.service.JwtService;
import cz.petrf.sraz.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final UserDetailsServiceImpl userDetailsService;
  private final JwtService jwtService;
  private final PasswordEncoder encoder;

  @PostMapping("/login")
  public ResponseEntity<?> createAuthenticationToken(@RequestBody LoginRequest loginRequest) {
    try {
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              loginRequest.getEmail(),
              loginRequest.getPassword()
          )
      );

      SecurityContextHolder.getContext().setAuthentication(authentication);

      final UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());

      final String jwt = jwtService.generateToken(userDetails);

      return ResponseEntity.ok(new JwtResponse(jwt));
    } catch (BadCredentialsException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }
  }
}

