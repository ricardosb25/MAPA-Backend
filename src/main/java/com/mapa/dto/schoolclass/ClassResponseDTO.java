package com.mapa.dto.schoolClass;

import com.mapa.domain.SchoolClass;
import com.mapa.domain.enums.Shift;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(name = "ClassResponse", description = "Resumo de uma turma exibido nas listagens")
public record ClassResponseDTO(

        @Schema(description = "Identificador da turma", example = "1")
        Long id,

        @Schema(description = "Nome da turma", example = "Eletronica Embarcada 2A")
        String name,

        @Schema(description = "Código único usado pelos alunos para entrar", example = "AUT-2A")
        String code,

        @Schema(description = "Turno da turma", example = "AFTERNOON")
        Shift shift,

        @Schema(description = "Nome do professor responsável", example = "Helena Duarte")
        String professorName,

        @Schema(description = "Quantidade de alunos matriculados", example = "2")
        long studentCount,

        @Schema(description = "Data de criação da turma")
        OffsetDateTime createdAt
) {

    public static ClassResponseDTO fromEntity(SchoolClass schoolClass, long studentCount) {
        return new ClassResponseDTO(
                schoolClass.getId(),
                schoolClass.getName(),
                schoolClass.getCode(),
                schoolClass.getShift(),
                schoolClass.getTeacher().getFullName(),
                studentCount,
                schoolClass.getCreatedAt());
    }
}