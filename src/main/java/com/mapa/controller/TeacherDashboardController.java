package com.mapa.controller;

import com.mapa.dto.dashboard.TeacherDashboardResponseDTO;
import com.mapa.service.TeacherDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "Dashboard do professor")
public class TeacherDashboardController {
    private final TeacherDashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Consultar desempenho das turmas do professor autenticado")
    public TeacherDashboardResponseDTO findDashboard() {
        return dashboardService.findDashboard();
    }
}
