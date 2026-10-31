package com.mapa.service;

import com.mapa.domain.User;
import com.mapa.domain.enums.Role;
import com.mapa.domain.enums.UserAuditAction;
import com.mapa.dto.user.EmailChangeRequestDTO;
import com.mapa.dto.user.PasswordChangeRequestDTO;
import com.mapa.exception.AccountSettingsException;
import com.mapa.exception.EmailAlreadyExistsException;
import com.mapa.exception.InvalidCredentialsException;
import com.mapa.repository.UserRepository;
import com.mapa.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountSettingsServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserAuditLogService auditLogService;
    @InjectMocks private AccountSettingsService settingsService;
    private User account;

    @BeforeEach
    void authenticateAccount() {
        account = User.builder().id(7L).fullName("Ana Silva").email("ana@mapa.test")
                .passwordHash("encoded-current").role(Role.STUDENT).active(true).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UserPrincipal(account), null, List.of()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNormalizeEmailAndAuditWithoutPassword() {
        allowCurrentPassword();
        account.setResetToken("pending-reset");
        settingsService.changeEmail(new EmailChangeRequestDTO(" NEW@mapa.test ", "current-password"));
        assertEquals("new@mapa.test", account.getEmail());
        assertNotNull(account.getEmailChangedAt());
        assertNull(account.getResetToken());
        verify(auditLogService).record(eq(UserAuditAction.EMAIL_CHANGED), eq(7L), eq("ana@mapa.test"),
                eq(7L), eq("new@mapa.test"), contains("ana@mapa.test -> new@mapa.test"));
    }

    @Test
    void shouldRejectSecondEmailChangeWithinMonth() {
        allowCurrentPassword();
        account.setEmailChangedAt(OffsetDateTime.now().minusDays(10));
        assertThrows(AccountSettingsException.class,
                () -> settingsService.changeEmail(new EmailChangeRequestDTO("new@mapa.test", "current-password")));
        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectDuplicateEmail() {
        allowCurrentPassword();
        when(userRepository.existsByEmail("used@mapa.test")).thenReturn(true);
        assertThrows(EmailAlreadyExistsException.class,
                () -> settingsService.changeEmail(new EmailChangeRequestDTO("used@mapa.test", "current-password")));
        assertEquals("ana@mapa.test", account.getEmail());
    }

    @Test
    void shouldRejectWrongCurrentPassword() {
        when(userRepository.findAccountForUpdate(7L)).thenReturn(Optional.of(account));
        assertThrows(AccountSettingsException.class,
                () -> settingsService.changePassword(new PasswordChangeRequestDTO("wrong-password", "new-password")));
        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldHashPasswordAndKeepIndependentEmailLimit() {
        allowCurrentPassword();
        account.setEmailChangedAt(OffsetDateTime.now());
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");
        settingsService.changePassword(new PasswordChangeRequestDTO("current-password", "new-password"));
        assertEquals("encoded-new", account.getPasswordHash());
        assertNotNull(account.getPasswordChangedAt());
        verify(auditLogService).record(eq(UserAuditAction.PASSWORD_CHANGED), eq(7L), eq("ana@mapa.test"),
                eq(7L), eq("ana@mapa.test"), eq("Senha alterada nas configurações da conta."));
    }

    @Test
    void shouldRejectPasswordChangeWithinMonth() {
        allowCurrentPassword();
        account.setPasswordChangedAt(OffsetDateTime.now().minusDays(20));
        assertThrows(AccountSettingsException.class,
                () -> settingsService.changePassword(new PasswordChangeRequestDTO("current-password", "new-password")));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldAllowPasswordChangeAfterCalendarMonth() {
        allowCurrentPassword();
        account.setPasswordChangedAt(OffsetDateTime.now().minusMonths(1).minusSeconds(1));
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");
        settingsService.changePassword(new PasswordChangeRequestDTO("current-password", "new-password"));
        verify(userRepository).saveAndFlush(account);
    }

    @Test
    void shouldRejectReusedPassword() {
        allowCurrentPassword();
        when(passwordEncoder.matches("new-password", "encoded-current")).thenReturn(true);
        assertThrows(AccountSettingsException.class,
                () -> settingsService.changePassword(new PasswordChangeRequestDTO("current-password", "new-password")));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldReturnNextAvailableDates() {
        account.setEmailChangedAt(OffsetDateTime.parse("2026-01-31T12:00:00Z"));
        when(userRepository.findById(7L)).thenReturn(Optional.of(account));
        var settings = settingsService.findSettings();
        assertEquals(OffsetDateTime.parse("2026-02-28T12:00:00Z"), settings.nextEmailChangeAt());
        assertNull(settings.nextPasswordChangeAt());
    }

    @Test
    void shouldRejectInactiveAccount() {
        account.setActive(false);
        when(userRepository.findById(7L)).thenReturn(Optional.of(account));
        assertThrows(InvalidCredentialsException.class, () -> settingsService.findSettings());
    }

    private void allowCurrentPassword() {
        when(userRepository.findAccountForUpdate(7L)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("current-password", "encoded-current")).thenReturn(true);
    }

    @Test
    void shouldRejectAdminPersonalSettings() {
        account.setRole(Role.ADMIN);
        when(userRepository.findById(7L)).thenReturn(Optional.of(account));
        when(userRepository.findAccountForUpdate(7L)).thenReturn(Optional.of(account));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> settingsService.findSettings());
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> settingsService.changeEmail(new EmailChangeRequestDTO("new@mapa.test", "current-password")));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> settingsService.changePassword(new PasswordChangeRequestDTO("current-password", "new-password")));
        verifyNoInteractions(passwordEncoder, auditLogService);
    }

    @Test
    void shouldAllowTeacherPersonalSettings() {
        account.setRole(Role.TEACHER);
        when(userRepository.findById(7L)).thenReturn(Optional.of(account));
        assertEquals("ana@mapa.test", settingsService.findSettings().email());
    }
}
