package com.mapa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "MotorSelection", description = "Motor ativo (selecionado na bancada) do usuário")
public record MotorSelectionRequestDTO(

        @NotNull(message = "O motor selecionado é obrigatório")
        @Schema(description = "Identificador do motor a tornar ativo", example = "1")
        Long engineId
) {
}
