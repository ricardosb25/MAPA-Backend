package com.mapa.dto.challenge;

import com.mapa.domain.ChallengeProgress;
import com.mapa.domain.ClassEnrollment;
import com.mapa.domain.enums.EngineStatus;
import com.mapa.service.ChallengeCriteria;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(name = "ChallengeStudentProgress", description = "Progresso de um aluno em um desafio")
public record ChallengeStudentProgressDTO(

        Long studentId,
        String fullName,
        String email,
        boolean selected,
        int attempts,
        boolean completed,
        int completedCriteria,
        Double lastPeakPower,
        EngineStatus lastEngineStatus,
        OffsetDateTime completedAt
) {

    public static ChallengeStudentProgressDTO fromEnrollment(
            ClassEnrollment enrollment,
            ChallengeProgress progress,
            int goalPowerCv,
            EngineStatus requiredEngineStatus) {

        int completedCriteria = progress == null
                ? 0
                : ChallengeCriteria.completedCount(
                        goalPowerCv,
                        requiredEngineStatus,
                        progress.getLastPeakPower(),
                        progress.getLastEngineStatus());

        return new ChallengeStudentProgressDTO(
                enrollment.getStudent().getId(),
                enrollment.getStudent().getFullName(),
                enrollment.getStudent().getEmail(),
                progress != null && progress.isSelected(),
                progress == null ? 0 : progress.getAttempts(),
                progress != null && progress.isCompleted(),
                completedCriteria,
                progress == null ? null : progress.getLastPeakPower(),
                progress == null ? null : progress.getLastEngineStatus(),
                progress == null ? null : progress.getCompletedAt());
    }
}
