package com.mapa.service;

import com.mapa.domain.User;
import com.mapa.domain.enums.Role;
import com.mapa.exception.InvalidCredentialsException;
import com.mapa.repository.TeacherDashboardRepository;
import com.mapa.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherDashboardServiceTest {
    @Mock private TeacherDashboardRepository dashboardRepository;
    @InjectMocks private TeacherDashboardService dashboardService;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldQueryOnlyAuthenticatedTeacher() {
        authenticate(Role.TEACHER, true);
        when(dashboardRepository.findClassPerformance(42L)).thenReturn(List.of());
        var dashboard = dashboardService.findDashboard();
        assertTrue(dashboard.classes().isEmpty());
        assertNotNull(dashboard.generatedAt());
        verify(dashboardRepository).findClassPerformance(42L);
    }

    @Test
    void shouldRejectMissingAuthentication() {
        SecurityContextHolder.clearContext();
        assertThrows(InvalidCredentialsException.class, () -> dashboardService.findDashboard());
        verifyNoInteractions(dashboardRepository);
    }

    @Test
    void shouldRejectStudentsAdminsAndInactiveTeachers() {
        for (Role role : List.of(Role.STUDENT, Role.ADMIN)) {
            authenticate(role, true);
            assertThrows(AccessDeniedException.class, () -> dashboardService.findDashboard());
        }
        authenticate(Role.TEACHER, false);
        assertThrows(AccessDeniedException.class, () -> dashboardService.findDashboard());
        verifyNoInteractions(dashboardRepository);
    }

    private void authenticate(Role role, boolean active) {
        User account = User.builder().id(42L).fullName("Professor de teste").email("teacher@mapa.test")
                .passwordHash("unused-test-hash").role(role).active(active).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UserPrincipal(account), null, List.of()));
    }
}
