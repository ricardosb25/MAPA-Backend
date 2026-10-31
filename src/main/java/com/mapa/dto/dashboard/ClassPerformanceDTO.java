package com.mapa.dto.dashboard;

import com.mapa.domain.enums.Shift;

public record ClassPerformanceDTO(
        Long classId,
        String className,
        String classCode,
        Shift shift,
        long studentCount,
        long challengeCount,
        long activeStudents,
        long totalAttempts,
        long completedAssignments,
        long inProgressAssignments,
        long notStartedAssignments,
        double completionRate) {
}
