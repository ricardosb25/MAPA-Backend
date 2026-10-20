package com.mapa.dto;

import com.mapa.domain.enums.EngineStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "DynoResult", description = "Resultado da simulação do Dinamômetro Virtual")
public record DynoResultDTO(

        @Schema(description = "Eixo X em RPM da puxada simulada")
        List<Integer> rpmAxis,

        @Schema(description = "Curva de potência (cv) por RPM")
        List<Double> powerCurve,

        @Schema(description = "Curva de torque (kgfm) por RPM")
        List<Double> torqueCurve,

        @Schema(description = "Curva de razão ar/combustível (AFR) por RPM")
        List<Double> afrCurve,

        @Schema(description = "Estado final do motor virtual", example = "INTEGRO")
        EngineStatus finalStatus,

        @Schema(description = "Pico de potência (cv)", example = "131.8")
        Double peakPower,

        @Schema(description = "Pico de torque (kgfm)", example = "15.7")
        Double peakTorque
) {
}
