package cz.petrf.sraz.security;

import cz.petrf.sraz.db.entity.User;
import cz.petrf.sraz.db.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Přihlášený uživatel z bezpečnostního kontextu (JWT).
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

  private final UserRepository userRepo;

  public User requireUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth!=null && auth.getPrincipal() instanceof AppUser appUser) {
      // znovu načíst – entita z filtru patří do jiné (už uzavřené) session
      return userRepo.findById(appUser.getDbUser().getId())
          .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Uživatel neexistuje"));
    }

    throw new AuthenticationCredentialsNotFoundException("Nepřihlášený uživatel");
  }
}
