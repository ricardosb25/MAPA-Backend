package com.mapa.service;

import com.mapa.dto.dashboard.TeacherDashboardResponseDTO;
import com.mapa.exception.InvalidCredentialsException;
import com.mapa.repository.TeacherDashboardRepository;
import com.mapa.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class TeacherDashboardService {
    private final TeacherDashboardRepository dashboardRepository;

    @Transactional(readOnly = true)
    public TeacherDashboardResponseDTO findDashboard() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new InvalidCredentialsException();
        }
        if (!principal.isActive() || !"TEACHER".equals(principal.getRole())) {
            throw new AccessDeniedException("A dashboard está disponível apenas para professores ativos.");
        }
        return new TeacherDashboardResponseDTO(dashboardRepository.findClassPerformance(principal.getId()),
                OffsetDateTime.now(ZoneOffset.UTC));
    }
}
