package com.mapa.dto.dashboard;

import java.time.OffsetDateTime;
import java.util.List;

public record TeacherDashboardResponseDTO(
        List<ClassPerformanceDTO> classes,
        OffsetDateTime generatedAt) {
}
