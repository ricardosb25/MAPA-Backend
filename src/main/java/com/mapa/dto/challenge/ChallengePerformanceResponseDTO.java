package com.mapa.dto.challenge;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ChallengePerformance", description = "Desempenho da turma em um desafio específico")
public record ChallengePerformanceResponseDTO(

        @Schema(description = "Dados do desafio avaliado")
        ChallengeResponseDTO challenge,

        @Schema(description = "Quantidade de alunos matriculados na turma", example = "6")
        long studentCount,

        @Schema(description = "Quantidade de alunos que escolheram o desafio", example = "3")
        long enrolledCount,

        @Schema(description = "Quantidade de alunos que concluíram o desafio", example = "2")
        long completedCount,

        @Schema(description = "Quantidade de alunos com o desafio ativo", example = "1")
        long activeCount,

        @Schema(description = "Total de tentativas registradas no desafio", example = "25")
        long totalAttempts,

        @Schema(description = "Percentual de alunos matriculados que concluíram o desafio", example = "33.3")
        double completionRate,

        @Schema(description = "Desempenho detalhado por aluno matriculado na turma")
        List<ChallengeStudentPerformanceDTO> students
) {
}