package com.mapa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(name = "EcuMap", description = "Mapa da ECU (ignição ou combustível) do motor ativo")
public record EcuMapResponseDTO(

        @Schema(description = "Identificador do motor dono do mapa", example = "1")
        Long motorId,

        @Schema(description = "Classe de litragem aproximada do motor (mapa genérico de fábrica)", example = "1.8")
        BigDecimal displacementClass,

        @Schema(description = "Tipo do mapa", example = "IGNITION")
        String mapType,

        @Schema(description = "Nome exibido do mapa", example = "Mapa de ignição")
        String label,

        @Schema(description = "Unidade dos valores", example = "graus de avanço")
        String unit,

        @Schema(description = "Eixo X em RPM")
        List<Integer> rpmBreakpoints,

        @Schema(description = "Eixo Y em carga do motor (%)")
        List<Integer> loadBreakpoints,

        @Schema(description = "Grade efetiva (fábrica ou editada pelo usuário)")
        List<List<Integer>> values,

        @Schema(description = "Grade original de fábrica")
        List<List<Integer>> defaultValues,

        @Schema(description = "Menor valor permitido para o mapa", example = "0")
        Integer minValue,

        @Schema(description = "Maior valor permitido para o mapa", example = "45")
        Integer maxValue,

        @Schema(description = "Incremento permitido", example = "1")
        Integer step,

        @Schema(description = "Indica que o mapa está no padrão de fábrica", example = "true")
        boolean factoryDefault,

        @Schema(description = "Data da última edição pelo usuário")
        OffsetDateTime updatedAt
) {
}
