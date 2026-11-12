package com.mapa.repository;

import com.mapa.dto.dashboard.ClassPerformanceDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TeacherDashboardRepositoryTest {
    private JdbcTemplate jdbcTemplate;
    private TeacherDashboardRepository dashboardRepository;

    @BeforeEach
    void prepareDatabase() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:dashboard-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbcTemplate = new JdbcTemplate(dataSource);
        dashboardRepository = new TeacherDashboardRepository(jdbcTemplate);
        jdbcTemplate.execute("CREATE TABLE school_classes (id BIGINT PRIMARY KEY, name VARCHAR(100), code VARCHAR(10), shift VARCHAR(20), teacher_id BIGINT)");
        jdbcTemplate.execute("CREATE TABLE class_enrollments (class_id BIGINT, student_id BIGINT, UNIQUE(class_id, student_id))");
        jdbcTemplate.execute("CREATE TABLE challenges (id BIGINT PRIMARY KEY, class_id BIGINT)");
        jdbcTemplate.execute("CREATE TABLE challenge_progress (challenge_id BIGINT, student_id BIGINT, attempts INT, completed BOOLEAN)");
        jdbcTemplate.update("""
                INSERT INTO school_classes VALUES
                  (1, 'Mecânica A', 'MEC-AA', 'MORNING', 42),
                  (2, 'Sem desafios', 'MEC-BB', 'EVENING', 42),
                  (3, 'Outro professor', 'MEC-CC', 'MORNING', 77),
                  (4, 'Sem alunos', 'MEC-DD', 'AFTERNOON', 42)
                """);
        jdbcTemplate.update("INSERT INTO class_enrollments VALUES (1, 11), (1, 12), (1, 13), (2, 11), (3, 21)");
        jdbcTemplate.update("INSERT INTO challenges VALUES (101, 1), (102, 1), (301, 3), (401, 4)");
        jdbcTemplate.update("""
                INSERT INTO challenge_progress VALUES
                  (101, 11, 2, TRUE), (102, 11, 1, FALSE), (101, 12, 0, FALSE),
                  (101, 99, 50, TRUE), (301, 21, 9, TRUE)
                """);
    }

    @AfterEach
    void cleanUpDatabase() {
        jdbcTemplate.execute("SHUTDOWN");
    }

    @Test
    void shouldAggregateWithoutMultiplyingCountsAndExcludeFormerStudents() {
        ClassPerformanceDTO performance = dashboardRepository.findClassPerformance(42L).stream()
                .filter(classPerformance -> classPerformance.classId() == 1L).findFirst().orElseThrow();
        assertEquals(3, performance.studentCount());
        assertEquals(2, performance.challengeCount());
        assertEquals(1, performance.activeStudents());
        assertEquals(3, performance.totalAttempts());
        assertEquals(1, performance.completedAssignments());
        assertEquals(1, performance.inProgressAssignments());
        assertEquals(4, performance.notStartedAssignments());
        assertEquals(16.7, performance.completionRate());
    }

    @Test
    void shouldRestrictClassesToTheirTeacher() {
        var teacherClasses = dashboardRepository.findClassPerformance(42L);
        assertEquals(3, teacherClasses.size());
        assertTrue(teacherClasses.stream().noneMatch(classPerformance -> classPerformance.classId() == 3L));
        assertEquals(100.0, dashboardRepository.findClassPerformance(77L).getFirst().completionRate());
        assertTrue(dashboardRepository.findClassPerformance(999L).isEmpty());
    }

    @Test
    void shouldReturnZeroRatesWhenStudentsOrChallengesAreMissing() {
        var emptyClasses = dashboardRepository.findClassPerformance(42L).stream()
                .filter(classPerformance -> classPerformance.classId() != 1L).toList();
        assertEquals(2, emptyClasses.size());
        for (ClassPerformanceDTO performance : emptyClasses) {
            assertEquals(0, performance.completionRate());
            assertEquals(0, performance.notStartedAssignments());
            assertEquals(0, performance.totalAttempts());
        }
    }

    @Test
    void shouldUseHistoricalCompletionFlagAndCountAnActiveStudentOnce() {
        jdbcTemplate.update("UPDATE challenge_progress SET completed = TRUE WHERE student_id = 11");
        ClassPerformanceDTO performance = dashboardRepository.findClassPerformance(42L).stream()
                .filter(classPerformance -> classPerformance.classId() == 1L).findFirst().orElseThrow();
        assertEquals(2, performance.completedAssignments());
        assertEquals(1, performance.activeStudents());
        assertEquals(0, performance.inProgressAssignments());
        assertEquals(33.3, performance.completionRate());
    }
}
