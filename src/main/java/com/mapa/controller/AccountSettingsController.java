package com.mapa.controller;

import com.mapa.dto.auth.MessageResponseDTO;
import com.mapa.dto.user.AccountSettingsResponseDTO;
import com.mapa.dto.user.EmailChangeRequestDTO;
import com.mapa.dto.user.PasswordChangeRequestDTO;
import com.mapa.service.AccountSettingsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account/settings")
@RequiredArgsConstructor
@Tag(name = "Configurações da conta")
@PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
public class AccountSettingsController {
    private final AccountSettingsService settingsService;

    @GetMapping
    public AccountSettingsResponseDTO findSettings() {
        return settingsService.findSettings();
    }

    @PutMapping("/email")
    public MessageResponseDTO changeEmail(@Valid @RequestBody EmailChangeRequestDTO request) {
        settingsService.changeEmail(request);
        return MessageResponseDTO.of("E-mail atualizado. Entre novamente com o novo e-mail.");
    }

    @PutMapping("/password")
    public MessageResponseDTO changePassword(@Valid @RequestBody PasswordChangeRequestDTO request) {
        settingsService.changePassword(request);
        return MessageResponseDTO.of("Senha atualizada. Entre novamente com a nova senha.");
    }
}
