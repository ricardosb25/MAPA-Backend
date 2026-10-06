package com.mapa.dto.schoolClass;

import com.mapa.domain.SchoolClass;
import com.mapa.domain.enums.Shift;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClassRequestDTO(

        @NotBlank(message = "Nome da turma é obrigatório")
        @Size(max = 100, message = "Nome da turma deve ter no máximo 100 caracteres")
        String name,

        @NotNull(message = "Turno da turma é obrigatório")
        Shift shift
) {

    public void applyTo(SchoolClass schoolClass) {
        schoolClass.setName(name.trim());
        schoolClass.setShift(shift);
    }
}