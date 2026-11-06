package com.mapa.dto.schoolClass;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClassJoinRequestDTO(

        @NotBlank(message = "Código da turma é obrigatório")
        @Size(max = 10, message = "Código da turma deve ter no máximo 10 caracteres")
        String code
) {
}