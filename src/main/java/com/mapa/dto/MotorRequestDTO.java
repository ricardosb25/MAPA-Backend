package com.mapa.dto;

import com.mapa.domain.Motor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(name = "MotorRequest", description = "Dados necessários para cadastrar ou atualizar um motor")
public record MotorRequestDTO(

        @NotBlank(message = "O fabricante é obrigatório")
        @Size(max = 50, message = "O fabricante deve ter no máximo 50 caracteres")
        @Schema(description = "Fabricante do motor", example = "Honda", maxLength = 50)
        String manufacturer,

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 50, message = "O nome deve ter no máximo 50 caracteres")
        @Schema(description = "Nome do motor", example = "CG 160 Titan", maxLength = 50)
        String name,

        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        @Schema(description = "Descrição do motor", example = "Motor monocilíndrico de uso urbano", maxLength = 255)
        String description,

        @NotNull(message = "A cilindrada em litros é obrigatória")
        @DecimalMin(value = "0.0", message = "A cilindrada em litros não pode ser negativa")
        @Digits(integer = 2, fraction = 1, message = "A cilindrada em litros deve ter no máximo 2 dígitos inteiros e 1 casa decimal")
        @Schema(description = "Cilindrada em litros", example = "0.2")
        BigDecimal displacementLiters,

        @NotNull(message = "A cilindrada em cc é obrigatória")
        @Positive(message = "A cilindrada em cc deve ser maior que zero")
        @Schema(description = "Cilindrada em centímetros cúbicos", example = "162")
        Integer displacementCc,

        @NotNull(message = "A taxa de compressão é obrigatória")
        @Positive(message = "A taxa de compressão deve ser maior que zero")
        @Digits(integer = 3, fraction = 1, message = "A taxa de compressão deve ter no máximo 3 dígitos inteiros e 1 casa decimal")
        @Schema(description = "Taxa de compressão", example = "9.5")
        BigDecimal compressionRatio,

        @NotNull(message = "O corte de giro (RPM) é obrigatório")
        @Positive(message = "O corte de giro (RPM) deve ser maior que zero")
        @Schema(description = "Rotação de corte do motor em RPM", example = "8500")
        Integer rpmCutoff,

        @NotBlank(message = "O tipo de aspiração é obrigatório")
        @Size(max = 20, message = "O tipo de aspiração deve ter no máximo 20 caracteres")
        @Schema(description = "Tipo de aspiração do motor", example = "Aspirado", maxLength = 20)
        String aspirationType
) {

    public void applyTo(Motor motor) {
        motor.setManufacturer(normalize(manufacturer));
        motor.setName(normalize(name));
        motor.setDescription(normalize(description));
        motor.setDisplacementLiters(displacementLiters);
        motor.setDisplacementCc(displacementCc);
        motor.setCompressionRatio(compressionRatio);
        motor.setRpmCutoff(rpmCutoff);
        motor.setAspirationType(normalize(aspirationType));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
