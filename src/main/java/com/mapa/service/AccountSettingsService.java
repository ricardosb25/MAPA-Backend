package com.mapa.service;

import com.mapa.domain.User;
import com.mapa.domain.enums.Role;
import com.mapa.domain.enums.UserAuditAction;
import com.mapa.dto.user.AccountSettingsResponseDTO;
import com.mapa.dto.user.EmailChangeRequestDTO;
import com.mapa.dto.user.PasswordChangeRequestDTO;
import com.mapa.exception.AccountSettingsException;
import com.mapa.exception.EmailAlreadyExistsException;
import com.mapa.exception.InvalidCredentialsException;
import com.mapa.repository.UserRepository;
import com.mapa.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AccountSettingsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAuditLogService auditLogService;

    @Transactional(readOnly = true)
    public AccountSettingsResponseDTO findSettings() {
        User account = userRepository.findById(currentUserId()).orElseThrow(InvalidCredentialsException::new);
        requireActive(account);
        return settingsFor(account);
    }

    @Transactional
    public void changeEmail(EmailChangeRequestDTO request) {
        User account = lockedAccount(request.currentPassword());
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (account.getEmail().equals(normalizedEmail)) {
            throw new AccountSettingsException("Informe um e-mail diferente do atual.");
        }
        requireAvailable(account.getEmailChangedAt(), "e-mail");
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }
        String previousEmail = account.getEmail();
        account.setEmail(normalizedEmail);
        account.setEmailChangedAt(OffsetDateTime.now(ZoneOffset.UTC));
        clearResetToken(account);
        userRepository.saveAndFlush(account);
        auditLogService.record(UserAuditAction.EMAIL_CHANGED, account.getId(), previousEmail,
                account.getId(), normalizedEmail, "E-mail alterado nas configurações da conta: "
                        + previousEmail + " -> " + normalizedEmail);
    }

    @Transactional
    public void changePassword(PasswordChangeRequestDTO request) {
        User account = lockedAccount(request.currentPassword());
        requireAvailable(account.getPasswordChangedAt(), "senha");
        if (passwordEncoder.matches(request.newPassword(), account.getPasswordHash())) {
            throw new AccountSettingsException("A nova senha deve ser diferente da atual.");
        }
        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        account.setPasswordChangedAt(OffsetDateTime.now(ZoneOffset.UTC));
        clearResetToken(account);
        userRepository.saveAndFlush(account);
        auditLogService.record(UserAuditAction.PASSWORD_CHANGED, account.getId(), account.getEmail(),
                account.getId(), account.getEmail(), "Senha alterada nas configurações da conta.");
    }

    private User lockedAccount(String currentPassword) {
        User account = userRepository.findAccountForUpdate(currentUserId())
                .orElseThrow(InvalidCredentialsException::new);
        requireActive(account);
        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new AccountSettingsException("A senha atual está incorreta.");
        }
        return account;
    }

    private Long currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new InvalidCredentialsException();
        }
        return principal.getId();
    }

    private void requireActive(User account) {
        if (!account.isActive()) {
            throw new InvalidCredentialsException();
        }
        if (account.getRole() != Role.STUDENT && account.getRole() != Role.TEACHER) {
            throw new AccessDeniedException("Configurações pessoais disponíveis apenas para alunos e professores.");
        }
    }

    private void requireAvailable(OffsetDateTime changedAt, String fieldLabel) {
        if (changedAt != null && OffsetDateTime.now(ZoneOffset.UTC).isBefore(changedAt.plusMonths(1))) {
            throw new AccountSettingsException("Você pode alterar " + fieldLabel
                    + " novamente a partir de " + changedAt.plusMonths(1) + ".");
        }
    }

    private AccountSettingsResponseDTO settingsFor(User account) {
        return new AccountSettingsResponseDTO(account.getEmail(),
                account.getEmailChangedAt() == null ? null : account.getEmailChangedAt().plusMonths(1),
                account.getPasswordChangedAt() == null ? null : account.getPasswordChangedAt().plusMonths(1));
    }

    private void clearResetToken(User account) {
        account.setResetToken(null);
        account.setResetTokenExpiresAt(null);
    }
}
