package cz.petrf.sraz.security;

import cz.petrf.sraz.db.entity.User;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class AppUser extends org.springframework.security.core.userdetails.User {

  @Getter
  private final User dbUser;

  /** Zablokovaný uživatel (blockedAt) je neaktivní – nepřihlásí se ani heslem, ani odkazem, ani starým JWT. */
  public AppUser(User dbUser) {
    this(dbUser, dbUser.getBlockedAt()==null, true, true, true);
  }

  public AppUser(User dbUser, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked) {
    super(dbUser.getEmail(), dbUser.getPassword(), enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, dbUser.getRoles().stream()
        .map(role -> new SimpleGrantedAuthority(role.getName()))
        .toList());

    this.dbUser = dbUser;
  }
}