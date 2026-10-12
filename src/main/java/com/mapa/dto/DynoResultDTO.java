package com.mapa.dto;

import com.mapa.domain.enums.EngineStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "DynoResult", description = "Resultado da simulação do Dinamômetro Virtual")
public record DynoResultDTO(

        @Schema(description = "Eixo X em RPM da puxada simulada")
        List<Integer> rpmEixoX,

        @Schema(description = "Curva de potência (cv) por RPM")
        List<Double> curvaPotencia,

        @Schema(description = "Curva de torque (kgfm) por RPM")
        List<Double> curvaTorque,

        @Schema(description = "Curva de razão ar/combustível (AFR) por RPM")
        List<Double> curvaAfr,

        @Schema(description = "Estado final do motor virtual", example = "INTEGRO")
        EngineStatus statusFinal,

        @Schema(description = "Pico de potência (cv)", example = "131.8")
        Double picoPotencia,

        @Schema(description = "Pico de torque (kgfm)", example = "15.7")
        Double picoTorque
) {
}
