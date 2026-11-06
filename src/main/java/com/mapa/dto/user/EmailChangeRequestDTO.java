package com.mapa.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailChangeRequestDTO(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 100) String currentPassword) {
}
