package cz.petrf.sraz.security;

import lombok.Data;

@Data
public class LoginRequest {
  private String username;
  private String password;
  private boolean remember;
}