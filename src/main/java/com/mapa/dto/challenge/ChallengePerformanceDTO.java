package com.mapa.dto.challenge;

import com.mapa.domain.Challenge;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ChallengePerformance", description = "Desempenho da turma em um desafio")
public record ChallengePerformanceDTO(

        Long challengeId,
        String title,
        String className,
        String classCode,
        Integer goalPowerCv,
        long enrolledStudents,
        long completedStudents,
        List<ChallengeStudentProgressDTO> students
) {

    public static ChallengePerformanceDTO fromChallenge(
            Challenge challenge, List<ChallengeStudentProgressDTO> students) {

        long completedStudents = students.stream().filter(ChallengeStudentProgressDTO::completed).count();
        return new ChallengePerformanceDTO(
                challenge.getId(),
                challenge.getTitle(),
                challenge.getSchoolClass().getName(),
                challenge.getSchoolClass().getCode(),
                challenge.getGoalPowerCv(),
                students.size(),
                completedStudents,
                students);
    }
}
