package com.antrigo.backend.dto.response;

public record JwtResponse(
        String token,
        String tokenType,
        String name,
        String email,
        String role
) {
    public static JwtResponse of(String token, String name, String email, String role) {
        return new JwtResponse(token, "Bearer", name, email, role);
    }
}
