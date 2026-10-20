package com.mapa.dto.challenge;

import com.mapa.domain.ChallengeAttempt;
import com.mapa.domain.enums.EngineStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(name = "ChallengeAttempt", description = "Puxada do dinamômetro registrada como tentativa de um desafio")
public record ChallengeAttemptResponseDTO(

        @Schema(description = "Identificador da tentativa", example = "1")
        Long id,

        @Schema(description = "Identificador do desafio", example = "1")
        Long challengeId,

        @Schema(description = "Título do desafio", example = "Falta de potência em alta rotação")
        String challengeTitle,

        @Schema(description = "Identificador do motor utilizado", example = "3")
        Long motorId,

        @Schema(description = "Valor alcançado na tentativa (cv ou kgfm)", example = "241")
        BigDecimal achievedValue,

        @Schema(description = "Estado do motor ao final da puxada", example = "INTEGRO")
        EngineStatus engineStatus,

        @Schema(description = "Indica se a tentativa bateu a meta do desafio", example = "true")
        boolean goalMet,

        @Schema(description = "Data e hora da tentativa")
        OffsetDateTime createdAt
) {

    public static ChallengeAttemptResponseDTO fromEntity(ChallengeAttempt attempt) {
        return new ChallengeAttemptResponseDTO(
                attempt.getId(),
                attempt.getChallenge().getId(),
                attempt.getChallenge().getTitle(),
                attempt.getMotorId(),
                attempt.getAchievedValue(),
                attempt.getEngineStatus(),
                attempt.isGoalMet(),
                attempt.getCreatedAt());
    }
}