package com.mapa.dto;

import com.mapa.domain.Motor;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MotorResponseDTO(
    Long id,
    String manufacturer,
    String name,
    String description,
    BigDecimal displacementLiters,
    Integer displacementCc,
    BigDecimal compressionRatio,
    Integer rpmCutoff,
    String aspirationType,
    OffsetDateTime createdAt
) {
    public static MotorResponseDTO fromEntity(Motor motor) {
        return new MotorResponseDTO(
            motor.getId(),
            motor.getManufacturer(),
            motor.getName(),
            motor.getDescription(),
            motor.getDisplacementLiters(),
            motor.getDisplacementCc(),
            motor.getCompressionRatio(),
            motor.getRpmCutoff(),
            motor.getAspirationType(),
            motor.getCreatedAt()
        );
    }
}
