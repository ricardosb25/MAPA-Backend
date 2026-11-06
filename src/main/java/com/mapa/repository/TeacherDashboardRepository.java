package com.mapa.repository;

import com.mapa.domain.enums.Shift;
import com.mapa.dto.dashboard.ClassPerformanceDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TeacherDashboardRepository {
    private final JdbcTemplate jdbcTemplate;

    public List<ClassPerformanceDTO> findClassPerformance(Long teacherId) {
        return jdbcTemplate.query("""
                WITH teacher_classes AS (
                    SELECT id, name, code, shift FROM school_classes WHERE teacher_id = ?
                ), enrollment_counts AS (
                    SELECT enrollment.class_id, COUNT(*) AS student_count
                    FROM class_enrollments enrollment
                    JOIN teacher_classes owned_class ON owned_class.id = enrollment.class_id
                    GROUP BY enrollment.class_id
                ), challenge_counts AS (
                    SELECT challenge.class_id, COUNT(*) AS challenge_count
                    FROM challenges challenge
                    JOIN teacher_classes owned_class ON owned_class.id = challenge.class_id
                    GROUP BY challenge.class_id
                ), activity_counts AS (
                    SELECT challenge.class_id,
                        SUM(progress.attempts) AS total_attempts,
                        COUNT(DISTINCT CASE WHEN progress.attempts > 0 OR progress.completed
                            THEN progress.student_id END) AS active_students,
                        SUM(CASE WHEN progress.completed THEN 1 ELSE 0 END) AS completed_count,
                        SUM(CASE WHEN NOT progress.completed AND progress.attempts > 0
                            THEN 1 ELSE 0 END) AS in_progress_count
                    FROM challenge_progress progress
                    JOIN challenges challenge ON challenge.id = progress.challenge_id
                    JOIN teacher_classes owned_class ON owned_class.id = challenge.class_id
                    JOIN class_enrollments enrollment ON enrollment.class_id = challenge.class_id
                        AND enrollment.student_id = progress.student_id
                    GROUP BY challenge.class_id
                )
                SELECT owned_class.id, owned_class.name, owned_class.code, owned_class.shift,
                    COALESCE(enrollments.student_count, 0) AS student_count,
                    COALESCE(challenges.challenge_count, 0) AS challenge_count,
                    COALESCE(activity.active_students, 0) AS active_students,
                    COALESCE(activity.total_attempts, 0) AS total_attempts,
                    COALESCE(activity.completed_count, 0) AS completed_count,
                    COALESCE(activity.in_progress_count, 0) AS in_progress_count
                FROM teacher_classes owned_class
                LEFT JOIN enrollment_counts enrollments ON enrollments.class_id = owned_class.id
                LEFT JOIN challenge_counts challenges ON challenges.class_id = owned_class.id
                LEFT JOIN activity_counts activity ON activity.class_id = owned_class.id
                ORDER BY LOWER(owned_class.name), owned_class.id
                """, this::mapClassPerformance, teacherId);
    }

    private ClassPerformanceDTO mapClassPerformance(ResultSet resultSet, int rowNumber) throws SQLException {
        long studentCount = resultSet.getLong("student_count");
        long challengeCount = resultSet.getLong("challenge_count");
        long expectedAssignments = studentCount * challengeCount;
        long completedAssignments = resultSet.getLong("completed_count");
        long inProgressAssignments = resultSet.getLong("in_progress_count");
        double completionRate = expectedAssignments == 0 ? 0
                : Math.round(completedAssignments * 1000.0 / expectedAssignments) / 10.0;
        return new ClassPerformanceDTO(resultSet.getLong("id"), resultSet.getString("name"),
                resultSet.getString("code"), Shift.valueOf(resultSet.getString("shift")),
                studentCount, challengeCount, resultSet.getLong("active_students"),
                resultSet.getLong("total_attempts"), completedAssignments, inProgressAssignments,
                Math.max(0, expectedAssignments - completedAssignments - inProgressAssignments), completionRate);
    }
}
