package com.mapa.dto.challenge;

import com.mapa.domain.ChallengeParticipation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(name = "ChallengeParticipation", description = "Vínculo de um aluno com um desafio (escolha, ativação e progresso)")
public record ChallengeParticipationResponseDTO(

        @Schema(description = "Identificador da participação", example = "1")
        Long id,

        @Schema(description = "Identificador do desafio", example = "1")
        Long challengeId,

        @Schema(description = "Identificador do aluno", example = "10")
        Long studentId,

        @Schema(description = "Indica se o desafio está ativo para o aluno", example = "true")
        boolean active,

        @Schema(description = "Melhor valor alcançado (cv ou kgfm)", example = "241")
        BigDecimal bestValue,

        @Schema(description = "Indica se o aluno já concluiu o desafio", example = "false")
        boolean completed,

        @Schema(description = "Data de conclusão do desafio (quando aplicável)")
        OffsetDateTime completedAt,

        @Schema(description = "Data em que o aluno escolheu o desafio")
        OffsetDateTime createdAt
) {

    public static ChallengeParticipationResponseDTO fromEntity(ChallengeParticipation participation) {
        return new ChallengeParticipationResponseDTO(
                participation.getId(),
                participation.getChallenge().getId(),
                participation.getStudent().getId(),
                participation.isActive(),
                participation.getBestValue(),
                participation.isCompleted(),
                participation.getCompletedAt(),
                participation.getCreatedAt());
    }
}