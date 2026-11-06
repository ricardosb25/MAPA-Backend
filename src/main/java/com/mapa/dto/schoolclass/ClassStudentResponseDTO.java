package com.mapa.dto.schoolClass;

import com.mapa.domain.ClassEnrollment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(name = "ClassStudentResponse", description = "Aluno matriculado em uma turma")
public record ClassStudentResponseDTO(

        @Schema(description = "Identificador do aluno", example = "10")
        Long studentId,

        @Schema(description = "Nome completo do aluno", example = "Diego Rocha")
        String fullName,

        @Schema(description = "E-mail do aluno", example = "diego@aluno.mapa.edu")
        String email,

        @Schema(description = "Data de matrícula na turma")
        OffsetDateTime joinedAt
) {

    public static ClassStudentResponseDTO fromEntity(ClassEnrollment schoolClassStudent) {
        return new ClassStudentResponseDTO(
                schoolClassStudent.getStudent().getId(),
                schoolClassStudent.getStudent().getFullName(),
                schoolClassStudent.getStudent().getEmail(),
                schoolClassStudent.getJoinedAt());
    }
}