package cz.petrf.sraz.exception;

public class NotFoundException extends DomainException {
  public NotFoundException(String what, Object id) {
    super("%s %s nebyl(a) nalezen(a).".formatted(what, id));
  }
}
