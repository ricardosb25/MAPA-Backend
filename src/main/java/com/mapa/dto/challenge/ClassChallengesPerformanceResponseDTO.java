package com.mapa.dto.challenge;

import com.mapa.domain.enums.ChallengeStudentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(name = "ClassChallengesPerformance",
        description = "Visão do desempenho da turma nos desafios, com filtro opcional por desafio")
public record ClassChallengesPerformanceResponseDTO(

        @Schema(description = "Identificador da turma", example = "1")
        Long classId,

        @Schema(description = "Nome da turma", example = "MEC-4B · Mecânica Automotiva 4B")
        String className,

        @Schema(description = "Identificador do desafio filtrado (null quando todos)", example = "null")
        Long challengeId,

        @Schema(description = "Percentual de conclusão das participações no escopo", example = "50.0")
        double completionRate,

        @Schema(description = "Quantidade de alunos que concluíram ao menos um desafio no escopo", example = "3")
        long studentsCompleted,

        @Schema(description = "Quantidade de alunos matriculados na turma", example = "6")
        long studentCount,

        @Schema(description = "Total de tentativas registradas no escopo", example = "37")
        long totalAttempts,

        @Schema(description = "Total de puxadas em que o motor não terminou íntegro", example = "7")
        long blownEngines,

        @Schema(description = "Taxa de conclusão de cada desafio no escopo")
        List<ChallengeCompletionDTO> challengeCompletion,

        @Schema(description = "Linhas de desempenho por aluno e desafio")
        List<ClassStudentChallengeRowDTO> rows
) {

        @Schema(name = "ChallengeCompletion", description = "Taxa de conclusão de um desafio específico")
        public record ChallengeCompletionDTO(

                @Schema(description = "Identificador do desafio", example = "1")
                Long challengeId,

                @Schema(description = "Título do desafio", example = "Falta de potência em alta rotação")
                String title,

                @Schema(description = "Quantidade de alunos que escolheram o desafio", example = "3")
                long enrolledCount,

                @Schema(description = "Quantidade de alunos que concluíram o desafio", example = "2")
                long completedCount,

                @Schema(description = "Percentual de conclusão entre os alunos matriculados", example = "33.3")
                double completionRate
        ) {

                public static ChallengeCompletionDTO of(
                        Long challengeId, String title, long enrolledCount, long completedCount, double completionRate) {
                        return new ChallengeCompletionDTO(challengeId, title, enrolledCount, completedCount, completionRate);
                }
        }

        @Schema(name = "ClassStudentChallengeRow",
                description = "Desempenho de um aluno em um desafio dentro da visão da turma")
        public record ClassStudentChallengeRowDTO(

                @Schema(description = "Identificador do aluno", example = "10")
                Long studentId,

                @Schema(description = "Nome completo do aluno", example = "Ana Lima")
                String studentName,

                @Schema(description = "Identificador do desafio", example = "1")
                Long challengeId,

                @Schema(description = "Título do desafio", example = "Falta de potência em alta rotação")
                String challengeTitle,

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
}