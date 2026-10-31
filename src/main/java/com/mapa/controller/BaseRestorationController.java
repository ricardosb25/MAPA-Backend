package com.mapa.controller;

import com.mapa.dto.auth.MessageResponseDTO;
import com.mapa.service.BaseRestorationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Restauração das bases")
public class BaseRestorationController {
    private final BaseRestorationService restorationService;

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
