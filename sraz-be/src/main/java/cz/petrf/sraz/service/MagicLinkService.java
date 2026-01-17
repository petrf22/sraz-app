package cz.petrf.sraz.service;

import cz.petrf.sraz.db.entity.MagicLinkToken;
import cz.petrf.sraz.db.entity.Role;
import cz.petrf.sraz.db.entity.User;
import cz.petrf.sraz.db.repo.MagicLinkTokenRepository;
import cz.petrf.sraz.db.repo.RoleRepository;
import cz.petrf.sraz.db.repo.UserRepository;
import cz.petrf.sraz.exception.ExpiredTokenException;
import cz.petrf.sraz.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MagicLinkService {

  private final MagicLinkTokenRepository magicLinkRepository;
  private final TemplateEngine templateEngine;
  private final EmailService emailService;
  private final UserRepository userRepo;
  private final RoleRepository roleRepo;
  @Value("${app.magic-link.expiration-minutes:15}")
  private int expirationMinutes;
  @Value("${app.magic-link.token-url:http://localhost:4200/verify-token/}")
  private String tokenUrl;
  @Value("${app.magic-link.mail.from:petr.franta@gmail.com}")
  private String fromEmail;
  @Value("${app.magic-link.mail.subject:Přání paní doktorce - přihlášení do aplikace}")
  private String mailSubject;

  @Transactional
  public String createMagicLink(String email) {
    // Generování magic linku
    String token = generateMagicLinkAndSave(email);

    return tokenUrl + token;
  }

  public void sendMagicLink(String magicLink, String email) {
    String htmlContent = createMagicLinkEmail(magicLink, email);
    log.info("createMagicLinkEmail - htmlContent size: {}", htmlContent.length());
    log.trace("htmlContent: {}", htmlContent);

    emailService.sendHtmlEmail(fromEmail, email, mailSubject, htmlContent);
  }

  private String createMagicLinkEmail(String magicLink, String email) {
    log.info("createMagicLinkEmail: email/magic-link-email");

    Context context = new Context();

    context.setVariable("magicLink", magicLink);
    context.setVariable("expirationMinutes", expirationMinutes);
    context.setVariable("userEmail", email);

    return templateEngine.process("email/magic-link-email", context);
  }

  private String generateMagicLinkAndSave(String email) {
    String token = UUID.randomUUID().toString();

    MagicLinkToken linkToken = new MagicLinkToken();

    linkToken.setEmail(email);
    linkToken.setToken(token);
    linkToken.setExpiresAt(OffsetDateTime.now().plusMinutes(expirationMinutes));

    magicLinkRepository.saveAndFlush(linkToken);

    return token;
  }

  @Transactional
  public MagicLinkToken verifyToken(String token) {
    MagicLinkToken linkToken = magicLinkRepository.findById(token).orElseThrow(InvalidTokenException::new);

    if (!linkToken.isValid()) {
      throw new ExpiredTokenException();
    }

    // Smaž token (jednorázové použití)
    magicLinkRepository.delete(linkToken);

    createUserIfNotExists(linkToken.getEmail());

    return linkToken;
  }

  private void createUserIfNotExists(String email) {
    Optional<User> userOpt = userRepo.findByEmail(email);

    if (userOpt.isEmpty()) {
      Role userRole = roleRepo.findByName("ROLE_USER").orElseThrow();
      User user = User.builder()
          .publicName(email)
          .password("")
          .email(email)
          .emailVerifiedAt(OffsetDateTime.now())
          .roles(Set.of(userRole))
          .build();

      userRepo.save(user);
    }
  }
}

