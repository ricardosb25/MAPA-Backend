package com.mapa.service;

import com.mapa.domain.User;
import com.mapa.domain.enums.Role;
import com.mapa.domain.enums.UserAuditAction;
import com.mapa.dto.auth.UserResponseDTO;
import com.mapa.dto.user.UserCreateRequestDTO;
import com.mapa.dto.user.UserUpdateRequestDTO;
import com.mapa.exception.RoleNotAllowedException;
import com.mapa.repository.UserRepository;
import com.mapa.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserAuditLogService userAuditLogService;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldBlockCommonUserFromReadingAnotherAccount() {
        User requestingUser = user(1L, "Ana Silva", "ana@email.com", Role.STUDENT);
        User otherUser = user(2L, "Carlos Souza", "carlos@email.com", Role.STUDENT);
        authenticateAs(requestingUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));

        assertThrows(AccessDeniedException.class, () -> userService.findById(2L));
    }

    @Test
    void shouldAllowAdminToReadAnyAccount() {
        User admin = user(9L, "Administrador", "admin@x.com", Role.ADMIN);
        User target = user(2L, "Carlos Souza", "carlos@email.com", Role.STUDENT);
        authenticateAs(admin);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        UserResponseDTO foundUser = userService.findById(2L);

        assertEquals("carlos@email.com", foundUser.email());
    }

    @Test
    void shouldBlockCommonUserFromUpdatingAnotherAccount() {
        User requestingUser = user(1L, "Ana Silva", "ana@email.com", Role.STUDENT);
        User otherUser = user(2L, "Carlos Souza", "carlos@email.com", Role.STUDENT);
        authenticateAs(requestingUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));

        assertThrows(AccessDeniedException.class,
                () -> userService.update(2L,
                        new UserUpdateRequestDTO("Carlos Editado", "carlos@email.com", null, null)));
    }

    @Test
    void shouldBlockCommonUserFromChangingOwnRole() {
        User student = user(1L, "Ana Silva", "ana@email.com", Role.STUDENT);
        authenticateAs(student);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));

        assertThrows(AccessDeniedException.class,
                () -> userService.update(1L,
                        new UserUpdateRequestDTO("Ana Silva", "ana@email.com", Role.ADMIN, null)));
    }

    @Test
    void shouldBlockCommonUserFromDeactivatingOwnAccount() {
        User student = user(1L, "Ana Silva", "ana@email.com", Role.STUDENT);
        authenticateAs(student);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));

        assertThrows(AccessDeniedException.class,
                () -> userService.update(1L,
                        new UserUpdateRequestDTO("Ana Silva", "ana@email.com", null, false)));
    }

    @Test
    void shouldUpdateOwnProfileAndAuditChanges() {
        User student = user(1L, "Ana Silva", "ana@email.com", Role.STUDENT);
        authenticateAs(student);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findByEmail("ana.souza@email.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO updatedUser = userService.update(1L,
                new UserUpdateRequestDTO("Ana Souza", "ana.souza@email.com", null, null));

        assertEquals("Ana Souza", updatedUser.fullName());
        assertEquals("ana.souza@email.com", updatedUser.email());
        verify(userAuditLogService).recordByCurrentUser(
                eq(UserAuditAction.USER_UPDATED), eq(1L), eq("ana.souza@email.com"), contains("fullName"));
    }

    @Test
    void shouldAuditAdminCreatingUser() {
        User admin = user(9L, "Administrador", "admin@x.com", Role.ADMIN);
        authenticateAs(admin);
        when(userRepository.findByEmail("novo@email.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senha123456")).thenReturn("hash-codificado");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO createdUser = userService.create(
                new UserCreateRequestDTO("Novo Aluno", "novo@email.com", "senha123456", Role.STUDENT));

        assertEquals("novo@email.com", createdUser.email());
        verify(userAuditLogService).recordByCurrentUser(
                eq(UserAuditAction.USER_CREATED), eq(createdUser.id()), eq("novo@email.com"),
                contains("admin@x.com"));
    }

    @Test
    void shouldBlockUserCreationForNonAdmin() {
        User student = user(1L, "Ana Silva", "ana@email.com", Role.STUDENT);
        authenticateAs(student);

        assertThrows(AccessDeniedException.class, () -> userService.create(
                new UserCreateRequestDTO("Novo Aluno", "novo@email.com", "senha123456", Role.STUDENT)));
        verify(userAuditLogService, never()).recordByCurrentUser(any(), any(), any(), any());
    }

    @Test
    void shouldBlockAnonymizingLastActiveAdmin() {
        User admin = user(9L, "Administrador", "admin@x.com", Role.ADMIN);
        authenticateAs(admin);
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndActiveTrueAndIdNot(Role.ADMIN, 9L)).thenReturn(0L);

        assertThrows(RoleNotAllowedException.class, () -> userService.anonymize(9L));

        verify(userRepository, never()).save(any(User.class));
        verify(userAuditLogService, never()).recordByCurrentUser(any(), any(), any(), any());
    }

    @Test
    void shouldAnonymizePersonalDataAndKeepTraceability() {
        User student = user(5L, "Ana Silva", "ana@email.com", Role.STUDENT);
        authenticateAs(student);
        when(userRepository.findById(5L)).thenReturn(Optional.of(student));
        when(passwordEncoder.encode(anyString())).thenReturn("hash-irreversivel");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.anonymize(5L);

        assertEquals("Usuário removido", student.getFullName());
        assertEquals(UserService.anonymizedEmailFor(5L), student.getEmail());
        assertFalse(student.isActive());
        assertNull(student.getResetToken());
        assertNull(student.getResetTokenExpiresAt());
        verify(userAuditLogService).recordByCurrentUser(
                eq(UserAuditAction.USER_ANONYMIZED), eq(5L), eq("ana@email.com"), contains("ana@email.com"));
    }

    private User user(Long id, String fullName, String email, Role role) {
        return User.builder()
                .id(id)
                .fullName(fullName)
                .email(email)
                .passwordHash("hash-original")
                .role(role)
                .active(true)
                .build();
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UserPrincipal(user),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
    }
}
