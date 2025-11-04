package cz.petrf.sraz.security;

import cz.petrf.sraz.db.entity.User;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class AppUser extends org.springframework.security.core.userdetails.User {

  @Getter
  private final User dbUser;

  public AppUser(User dbUser) {
    super(dbUser.getEmail(), dbUser.getPassword(), dbUser.getRoles().stream()
        .map(role -> new SimpleGrantedAuthority(role.getName()))
        .toList());

    this.dbUser = dbUser;
  }

  public AppUser(User dbUser, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked) {
    super(dbUser.getEmail(), dbUser.getPassword(), enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, dbUser.getRoles().stream()
        .map(role -> new SimpleGrantedAuthority(role.getName()))
        .toList());

    this.dbUser = dbUser;
  }
}
