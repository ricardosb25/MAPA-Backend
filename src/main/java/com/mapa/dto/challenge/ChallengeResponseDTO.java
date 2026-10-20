package com.mapa.dto.challenge;

import com.mapa.domain.Challenge;
import com.mapa.domain.ChallengeProgress;
import com.mapa.domain.enums.ChallengeDifficulty;
import com.mapa.domain.enums.EngineStatus;
import com.mapa.service.ChallengeCriteria;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(name = "ChallengeResponse", description = "Desafio de calibração atribuído a uma turma")
public record ChallengeResponseDTO(

        Long id,
        String title,
        String description,
        ChallengeDifficulty difficulty,
        Long motorId,
        String motorManufacturer,
        String motorName,
        Long classId,
        String className,
        String classCode,
        Integer goalPowerCv,
        EngineStatus requiredEngineStatus,
        OffsetDateTime createdAt,
        boolean selected,
        int attempts,
        boolean completed,
        int completedCriteria,
        List<String> criteria,
        long completedStudents,
        long enrolledStudents
) {

    public static ChallengeResponseDTO fromEntity(
            Challenge challenge,
            ChallengeProgress progress,
            long completedStudents,
            long enrolledStudents) {

        EngineStatus requiredEngineStatus = challenge.getRequiredEngineStatus() == null
                ? EngineStatus.INTEGRO
                : challenge.getRequiredEngineStatus();
        List<String> criteria = ChallengeCriteria.labels(challenge.getGoalPowerCv(), requiredEngineStatus);
        int completedCriteria = progress == null
                ? 0
                : ChallengeCriteria.completedCount(
                        challenge.getGoalPowerCv(),
                        requiredEngineStatus,
                        progress.getLastPeakPower(),
                        progress.getLastEngineStatus());

        return new ChallengeResponseDTO(
                challenge.getId(),
                challenge.getTitle(),
                challenge.getDescription(),
                challenge.getDifficulty(),
                challenge.getMotor().getId(),
                challenge.getMotor().getManufacturer(),
                challenge.getMotor().getName(),
                challenge.getSchoolClass().getId(),
                challenge.getSchoolClass().getName(),
                challenge.getSchoolClass().getCode(),
                challenge.getGoalPowerCv(),
                requiredEngineStatus,
                challenge.getCreatedAt(),
                progress != null && progress.isSelected(),
                progress == null ? 0 : progress.getAttempts(),
                progress != null && progress.isCompleted(),
                completedCriteria,
                criteria,
                completedStudents,
                enrolledStudents);
    }
}
