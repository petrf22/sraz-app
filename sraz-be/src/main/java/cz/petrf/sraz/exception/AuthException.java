package cz.petrf.sraz.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Chyba přihlašování – {@code code} čte frontend, {@code message} je pro uživatele.
 */
@Getter
public class AuthException extends RuntimeException {

  private final String code;
  private final HttpStatus status;

  public AuthException(String code, HttpStatus status, String message) {
    super(message);
    this.code = code;
    this.status = status;
  }

  /** Neplatný, prošlý nebo vyčerpaný kód – schválně jedna zpráva pro všechny případy. */
  public static AuthException invalidChallenge() {
    return new AuthException("INVALID_CHALLENGE", HttpStatus.BAD_REQUEST, "Kód je neplatný nebo vypršel.");
  }

  public static AuthException tooManyRequests() {
    return new AuthException("TOO_MANY_REQUESTS", HttpStatus.TOO_MANY_REQUESTS, "Příliš mnoho pokusů, zkuste to prosím později.");
  }

  public static AuthException sessionExpired() {
    return new AuthException("SESSION_EXPIRED", HttpStatus.UNAUTHORIZED, "Přihlášení vypršelo, přihlaste se prosím znovu.");
  }

  public static AuthException blocked() {
    return new AuthException("ACCOUNT_BLOCKED", HttpStatus.FORBIDDEN, "Účet je zablokovaný.");
  }

  public static AuthException termsRequired() {
    return new AuthException("TERMS_ACCEPTANCE_REQUIRED", HttpStatus.BAD_REQUEST, "Pro vytvoření účtu je potřeba souhlas se zpracováním údajů.");
  }
}
