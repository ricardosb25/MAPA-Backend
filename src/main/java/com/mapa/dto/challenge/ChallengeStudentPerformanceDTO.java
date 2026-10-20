package com.mapa.dto.challenge;

import com.mapa.domain.enums.ChallengeStudentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(name = "ChallengeStudentPerformance",
        description = "Desempenho de um aluno em um desafio, visto pelo professor")
public record ChallengeStudentPerformanceDTO(

        @Schema(description = "Identificador do aluno", example = "10")
        Long studentId,

        @Schema(description = "Nome completo do aluno", example = "Ana Lima")
        String studentName,

        @Schema(description = "E-mail do aluno", example = "ana@aluno.mapa.edu")
        String studentEmail,

        @Schema(description = "Quantidade de tentativas registradas", example = "8")
        long attempts,

        @Schema(description = "Melhor valor alcançado (cv ou kgfm)", example = "241")
        BigDecimal bestValue,

        @Schema(description = "Situação do aluno no desafio", example = "COMPLETED")
        ChallengeStudentStatus status,

        @Schema(description = "Data de conclusão do desafio (quando aplicável)")
        OffsetDateTime completedAt
) {
}