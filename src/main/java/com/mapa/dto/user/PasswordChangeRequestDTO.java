package com.mapa.dto.user;

import com.mapa.validation.BcryptPassword;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequestDTO(
        @NotBlank @Size(max = 100) String currentPassword,
        @NotBlank @Size(min = 8) @BcryptPassword String newPassword) {
}
