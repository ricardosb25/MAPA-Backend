package com.mapa.dto.auth;

import com.mapa.domain.User;
import com.mapa.domain.enums.Role;

public record UserResponseDTO(
        Long id,
        String fullName,
        String email,
        Role role,
        boolean active
) {
    public static UserResponseDTO fromEntity(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isActive()
        );
    }
}
