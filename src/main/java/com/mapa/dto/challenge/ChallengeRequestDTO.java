package com.mapa.dto.challenge;

import com.mapa.domain.Challenge;
import com.mapa.domain.enums.ChallengeDifficulty;
import com.mapa.domain.enums.EngineStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChallengeRequestDTO(

        @NotBlank(message = "Título do desafio é obrigatório")
        @Size(max = 120, message = "Título do desafio deve ter no máximo 120 caracteres")
        String title,

        @NotBlank(message = "Descrição do desafio é obrigatória")
        @Size(max = 255, message = "Descrição do desafio deve ter no máximo 255 caracteres")
        String description,

        @NotNull(message = "Dificuldade do desafio é obrigatória")
        ChallengeDifficulty difficulty,

        @NotNull(message = "Motor do desafio é obrigatório")
        Long motorId,

        @NotNull(message = "Turma do desafio é obrigatória")
        Long classId,

        @NotNull(message = "Meta de potência é obrigatória")
        @Positive(message = "Meta de potência deve ser maior que zero")
        Integer goalPowerCv,

        @NotNull(message = "Status final do motor é obrigatório")
        EngineStatus requiredEngineStatus
) {

    public void applyTo(Challenge challenge) {
        challenge.setTitle(title.trim());
        challenge.setDescription(description.trim());
        challenge.setDifficulty(difficulty);
        challenge.setGoalPowerCv(goalPowerCv);
        challenge.setRequiredEngineStatus(requiredEngineStatus);
    }
}
