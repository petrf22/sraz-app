package cz.petrf.sraz.security;

import java.util.List;

public record TokenDto(String accessToken, String email, List<String> roles) {
}