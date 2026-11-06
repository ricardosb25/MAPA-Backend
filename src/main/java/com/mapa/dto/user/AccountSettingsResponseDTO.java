package com.mapa.dto.user;

import java.time.OffsetDateTime;

public record AccountSettingsResponseDTO(
        String email,
        OffsetDateTime nextEmailChangeAt,
        OffsetDateTime nextPasswordChangeAt) {
}
