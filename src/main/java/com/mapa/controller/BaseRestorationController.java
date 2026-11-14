package com.mapa.controller;

import com.mapa.dto.auth.MessageResponseDTO;
import com.mapa.service.BaseRestorationService;
import com.mapa.service.PresentationModeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Restauração das bases")
public class BaseRestorationController {
    private final BaseRestorationService restorationService;
    private final PresentationModeService presentationModeService;

    @GetMapping("/presentation-mode")
    public PresentationModeService.Status getPresentationMode() {
        return presentationModeService.getStatus();
    }

    @PutMapping("/presentation-mode/{enabled}")
    public PresentationModeService.Status setPresentationMode(@PathVariable boolean enabled) {
        return presentationModeService.setEnabled(enabled);
    }

    @PostMapping("/restore-motors")
    public MessageResponseDTO restoreMotors() {
        restorationService.restoreMotors();
        return MessageResponseDTO.of("Base de motores restaurada com sucesso.");
    }

    @PostMapping("/restore-maps")
    public MessageResponseDTO restoreMaps() {
        restorationService.restoreMaps();
        return MessageResponseDTO.of("Base de mapas restaurada com sucesso.");
    }
}
