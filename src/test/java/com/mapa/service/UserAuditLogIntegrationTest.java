package com.mapa.service;

import com.mapa.domain.User;
import com.mapa.domain.UserAuditLog;
import com.mapa.domain.enums.Role;
import com.mapa.domain.enums.UserAuditAction;
import com.mapa.dto.PageResponseDTO;
import com.mapa.dto.auth.RegisterRequestDTO;
import com.mapa.dto.auth.ResetPasswordRequestDTO;
import com.mapa.dto.auth.UserResponseDTO;
import com.mapa.dto.user.UserAuditLogResponseDTO;
import com.mapa.dto.user.UserCreateRequestDTO;
import com.mapa.dto.user.UserUpdateRequestDTO;
import com.mapa.repository.UserAuditLogRepository;
import com.mapa.repository.UserRepository;
import com.mapa.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserAuditLogIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@mapa.test";

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserAuditLogService userAuditLogService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAuditLogRepository userAuditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPersistUserCreatedLogOnSelfRegistration() {
        String email = uniqueEmail();

        UserResponseDTO registeredUser =
                authService.register(new RegisterRequestDTO("Ana Silva", email, "password123", Role.STUDENT));

        assertTrue(hasLog(UserAuditAction.USER_CREATED, registeredUser.id()));
        assertTrue(logsFor(registeredUser.id()).stream()
                .anyMatch(entry -> email.equals(entry.getTargetEmail())));
    }

    @Test
    void shouldPersistUserCreatedLogWithAdminAsActor() {
        User admin = authenticateAsAdmin();
        String email = uniqueEmail();

        UserResponseDTO createdUser = userService.create(
                new UserCreateRequestDTO("Novo Aluno", email, "password123", Role.STUDENT));

        assertTrue(logsFor(createdUser.id()).stream().anyMatch(entry ->
                entry.getAction() == UserAuditAction.USER_CREATED
                        && admin.getEmail().equals(entry.getActorEmail())));
    }

    @Test
    void shouldPersistUserUpdatedLogWithChangedFields() {
        String email = uniqueEmail();
        UserResponseDTO registeredUser =
                authService.register(new RegisterRequestDTO("Ana Silva", email, "password123", Role.STUDENT));
        authenticateAs(userRepository.findById(registeredUser.id()).orElseThrow());

        userService.update(registeredUser.id(),
                new UserUpdateRequestDTO("Ana Souza", email, null, null));

        assertTrue(logsFor(registeredUser.id()).stream().anyMatch(entry ->
                entry.getAction() == UserAuditAction.USER_UPDATED
                        && entry.getDetails() != null
                        && entry.getDetails().contains("fullName")));
    }

    @Test
    void shouldPersistPasswordResetLog() {
        String resetToken = "token-auditoria-" + UUID.randomUUID();
        User user = userRepository.save(User.builder()
                .fullName("Carla Reset")
                .email(uniqueEmail())
                .passwordHash(passwordEncoder.encode("senhaAntiga123"))
                .role(Role.STUDENT)
                .active(true)
                .resetToken(resetToken)
                .resetTokenExpiresAt(OffsetDateTime.now().plusMinutes(30))
                .build());

        authService.resetPassword(new ResetPasswordRequestDTO(resetToken, "senhaNova12345"));

        assertTrue(hasLog(UserAuditAction.PASSWORD_RESET, user.getId()));
    }

    @Test
    void shouldPersistAnonymizationLogWithOriginalEmail() {
        String email = uniqueEmail();
        UserResponseDTO registeredUser =
                authService.register(new RegisterRequestDTO("Ana Silva", email, "password123", Role.STUDENT));
        authenticateAs(userRepository.findById(registeredUser.id()).orElseThrow());

        userService.anonymize(registeredUser.id());

        assertTrue(logsFor(registeredUser.id()).stream().anyMatch(entry ->
                entry.getAction() == UserAuditAction.USER_ANONYMIZED
                        && email.equals(entry.getTargetEmail())));
    }

    @Test
    void shouldFilterAuditLogsByActionAndTargetUser() {
        String email = uniqueEmail();
        UserResponseDTO registeredUser =
                authService.register(new RegisterRequestDTO("Ana Silva", email, "password123", Role.STUDENT));

        PageResponseDTO<UserAuditLogResponseDTO> filteredPage = userAuditLogService.findAll(
                UserAuditAction.USER_CREATED, registeredUser.id(), PageRequest.of(0, 10));

        assertFalse(filteredPage.content().isEmpty());
        filteredPage.content().forEach(entry -> {
            assertEquals(UserAuditAction.USER_CREATED, entry.action());
            assertEquals(registeredUser.id(), entry.targetUserId());
            assertNotNull(entry.createdAt());
        });
    }

    @Test
    void shouldReturnAuditLogsOrderedByMostRecentFirst() {
        authService.register(new RegisterRequestDTO("Ana Silva", uniqueEmail(), "password123", Role.STUDENT));

        PageResponseDTO<UserAuditLogResponseDTO> page =
                userAuditLogService.findAll(null, null, PageRequest.of(0, 50));

        assertFalse(page.content().isEmpty());
        for (int index = 1; index < page.content().size(); index++) {
            assertFalse(page.content().get(index - 1).createdAt().isBefore(page.content().get(index).createdAt()),
                    "Registros de auditoria devem estar ordenados do mais recente para o mais antigo");
        }
    }

    private boolean hasLog(UserAuditAction action, Long targetUserId) {
        return logsFor(targetUserId).stream().anyMatch(entry -> entry.getAction() == action);
    }

    private List<UserAuditLog> logsFor(Long targetUserId) {
        return userAuditLogRepository.findAll().stream()
                .filter(entry -> targetUserId.equals(entry.getTargetUserId()))
                .toList();
    }

    private String uniqueEmail() {
        return "auditoria-" + UUID.randomUUID() + "@email.com";
    }

    private User authenticateAsAdmin() {
        User admin = userRepository.findByEmail(ADMIN_EMAIL)
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("Administrador MAPA")
                        .email(ADMIN_EMAIL)
                        .passwordHash(passwordEncoder.encode("AdminTest12345!"))
                        .role(Role.ADMIN)
                        .active(true)
                        .build()));
        authenticateAs(admin);
        return admin;
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UserPrincipal(user),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
    }
}
