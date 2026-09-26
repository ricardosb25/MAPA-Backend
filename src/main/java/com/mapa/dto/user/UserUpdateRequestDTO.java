package com.mapa.dto.user;

import com.mapa.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequestDTO(

        @NotBlank(message = "Nome completo é obrigatório")
        @Size(min = 3, max = 100, message = "Nome completo deve ter entre 3 e 100 caracteres")
        String fullName,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "E-mail deve ter no máximo 150 caracteres")
        String email,

        Role role,

        Boolean active
) {
}
