package cz.petrf.sraz.db.seed;

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
        if (users.count() != 0) return;          // už seedováno

        Role adminRole = roles.save(new Role("admin"));
        Role userRole  = roles.save(new Role("user"));

        User root = User.builder()
                .publicName("Root Admin")
                .email("admin@example.com")
                .password(encoder.encode("admin"))
                .emailVerifiedAt(Instant.now())
                .roles(Set.of(adminRole))
                .build();
        users.save(root);

        // 50 náhodných uživatelů
        Faker faker = new Faker(new Locale("cs"));
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