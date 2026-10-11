package com.mapa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "EcuMapCellUpdate", description = "Atualização de uma célula do mapa da ECU")
public record EcuMapCellUpdateRequestDTO(

        @NotNull(message = "O índice de carga é obrigatório")
        @Schema(description = "Índice da linha de carga (eixo Y)", example = "0")
        Integer loadIndex,

        @NotNull(message = "O índice de RPM é obrigatório")
        @Schema(description = "Índice da coluna de RPM (eixo X)", example = "3")
        Integer rpmIndex,

        @NotNull(message = "O valor da célula é obrigatório")
        @Schema(description = "Novo valor da célula", example = "12")
        Integer value
) {
}
