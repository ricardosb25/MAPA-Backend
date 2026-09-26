package com.mapa.dto.auth;

public record AuthResponseDTO(
        String token,
        String type,
        long expiresIn,
        UserResponseDTO user
) {
    public static AuthResponseDTO bearer(String token, long expiresInSeconds, UserResponseDTO user) {
        return new AuthResponseDTO(token, "Bearer", expiresInSeconds, user);
    }
}

