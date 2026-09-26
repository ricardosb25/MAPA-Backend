package com.mapa.dto.user;

import com.mapa.domain.UserAuditLog;
import com.mapa.domain.enums.UserAuditAction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(name = "UserAuditLog", description = "Registro de auditoria de uma operação realizada sobre um usuário")
public record UserAuditLogResponseDTO(

        @Schema(description = "Identificador do registro de auditoria", example = "10")
        Long id,

        @Schema(description = "Ação auditada", example = "USER_UPDATED")
        UserAuditAction action,

        @Schema(description = "Identificador do usuário que executou a ação (nulo em ações anônimas)", example = "1")
        Long actorId,

        @Schema(description = "E-mail do usuário que executou a ação no momento da ação", example = "admin@mapa.test")
        String actorEmail,

        @Schema(description = "Identificador do usuário alvo da ação", example = "7")
        Long targetUserId,

        @Schema(description = "E-mail do usuário alvo congelado no momento da ação", example = "ana@email.com")
        String targetEmail,

        @Schema(description = "Detalhes da operação (diferenças aplicadas, contexto)", example = "fullName: 'Ana' -> 'Ana Souza'")
        String details,

        @Schema(description = "Data e hora da ação", example = "2026-01-01T12:00:00Z")
        OffsetDateTime createdAt
) {
    public static UserAuditLogResponseDTO fromEntity(UserAuditLog userAuditLog) {
        return new UserAuditLogResponseDTO(
                userAuditLog.getId(),
                userAuditLog.getAction(),
                userAuditLog.getActorId(),
                userAuditLog.getActorEmail(),
                userAuditLog.getTargetUserId(),
                userAuditLog.getTargetEmail(),
                userAuditLog.getDetails(),
                userAuditLog.getCreatedAt()
        );
    }
}
