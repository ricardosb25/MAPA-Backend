package com.mapa.dto;

import com.mapa.domain.DynamometerTest;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(name = "DynoTest", description = "Tentativa de puxada registrada no dinamômetro virtual")
public record DynoTestResponseDTO(

        @Schema(description = "Identificador do teste", example = "1")
        Long id,

        @Schema(description = "Identificador do motor simulado", example = "1")
        Long motorId,

        @Schema(description = "Pico de potência atingido (cv)", example = "131.8")
        Double maxPower,

        @Schema(description = "Pico de torque atingido (kgfm)", example = "15.7")
        Double maxTorque,

        @Schema(description = "Razão ar/combustível média da puxada", example = "14.1")
        Double averageAfr,

        @Schema(description = "Estado final do motor virtual", example = "INTEGRO")
        String finalStatus,

        @Schema(description = "Data e hora da simulação")
        OffsetDateTime testedAt
) {

    public static DynoTestResponseDTO fromEntity(DynamometerTest test) {
        return new DynoTestResponseDTO(
                test.getId(),
                test.getMotorId(),
                test.getMaxPower(),
                test.getMaxTorque(),
                test.getAverageAfr(),
                test.getEngineStatus().name(),
                test.getTestedAt());
    }
}
