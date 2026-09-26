package com.mapa.dto.auth;

import com.mapa.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(

        @NotBlank(message = "Nome completo é obrigatório")
        @Size(min = 3, max = 100, message = "Nome completo deve ter entre 3 e 100 caracteres")
        String fullName,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 150, message = "E-mail deve ter no máximo 150 caracteres")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres")
        String password,

        @NotNull(message = "Perfil é obrigatório")
        Role role,

        @NotNull(message = "É necessário aceitar os Termos de Uso para se cadastrar")
        Boolean acceptedTerms
) {
}
