package cz.petrf.sraz.exception;

/**
 * Porušení doménového pravidla – zpráva je určena přímo uživateli (česky).
 */
public class DomainException extends RuntimeException {
  public DomainException(String message) {
    super(message);
  }
}
