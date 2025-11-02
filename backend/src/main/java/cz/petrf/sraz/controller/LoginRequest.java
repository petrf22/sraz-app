package cz.petrf.sraz.controller;

import lombok.Data;

@Data
// Request a response DTO
public class LoginRequest {
  private String email;
  private String password;
  // Gettery a settery
}
