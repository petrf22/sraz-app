// SecurityConfig.java (nebo jakákoli @Configuration)
package cz.petrf.sraz.config;

import cz.petrf.sraz.security.JwtRequestFilter;
import cz.petrf.sraz.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Autowired
  private JwtRequestFilter jwtRequestFilter;

  @Autowired
  private UserDetailsServiceImpl userDetailsService;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);   // strength 10
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/graphql", "/graphiql", "/vendor/graphiql/**").permitAll()
//            .anyRequest().permitAll()
                .anyRequest().authenticated()
        )
        .sessionManagement(sess -> sess
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

    http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

//  @Bean
//  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//    http
//        .csrf(csrf -> csrf.disable())
//        .authorizeHttpRequests(auth -> auth
//            .requestMatchers("/graphql", "/graphiql", "/vendor/graphiql/**").permitAll()
//            .anyRequest().permitAll()
//        );
//    return http.build();
//  }

//  @Bean
//  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//    http
//        .csrf(csrf -> csrf.disable())                 // GraphQL = POST → CSRF off
//        .authorizeHttpRequests(auth -> auth
//            .requestMatchers(antMatcher("/graphql")).permitAll() // povolíme, řešíme v @PreAuthorize
//            .anyRequest().permitAll()
//        )
//        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//        .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
//    return http.build();
//  }
}