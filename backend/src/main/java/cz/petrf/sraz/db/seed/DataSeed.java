package cz.petrf.sraz.db.seed;

import com.github.javafaker.Faker;
import cz.petrf.sraz.db.entity.Role;
import cz.petrf.sraz.db.entity.User;
import cz.petrf.sraz.db.repo.RoleRepository;
import cz.petrf.sraz.db.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class DataSeed {

  private final UserRepository users;
  private final RoleRepository roles;
  private final PasswordEncoder encoder;

  @EventListener
  public void onAppReady(ApplicationReadyEvent ev) {
    if (users.count() > 50) return;          // už seedováno

    Role adminRole = roles.findByName("ROLE_ADMIN")
        .orElseGet(() -> roles.save(Role.builder().name("ROLE_ADMIN").build()));
    Role userRole = roles.findByName("ROLE_USER")
        .orElseGet(() -> roles.save(Role.builder().name("ROLE_USER").build()));

    User root = User.builder()
        .publicName("Root Admin Seed")
        .firstName("Root")
        .lastName("Admin Seed")
        .email("admin-seed@example.com")
        .password(encoder.encode("admin"))
        .emailVerifiedAt(Instant.now())
        .roles(Set.of(adminRole))
        .build();
    users.save(root);

    // 50 náhodných uživatelů
    Faker faker = new Faker(Locale.of("cs"));
    IntStream.rangeClosed(1, 50).forEach(i -> {
      User u = User.builder()
          .publicName(faker.name().fullName())
          .firstName(faker.name().firstName())
          .lastName(faker.name().lastName())
          .email(faker.internet().emailAddress())
          .password(encoder.encode("password"))
          .roles(Set.of(userRole))
          .build();
      users.save(u);
    });
  }
}