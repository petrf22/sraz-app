package cz.petrf.sraz.exception;

public class InvalidEmailDomainException extends EmailException {

  public InvalidEmailDomainException(String message) {
    super(message);
  }
}
