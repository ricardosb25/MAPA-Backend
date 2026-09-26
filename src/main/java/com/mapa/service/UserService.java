package com.mapa.service;

import com.mapa.domain.User;
import com.mapa.domain.enums.Role;
import com.mapa.domain.enums.UserAuditAction;
import com.mapa.dto.PageResponseDTO;
import com.mapa.dto.auth.UserResponseDTO;
import com.mapa.dto.user.UserCreateRequestDTO;
import com.mapa.dto.user.UserUpdateRequestDTO;
import com.mapa.exception.EmailAlreadyExistsException;
import com.mapa.exception.InvalidCredentialsException;
import com.mapa.exception.ResourceNotFoundException;
import com.mapa.exception.RoleNotAllowedException;
import com.mapa.repository.UserRepository;
import com.mapa.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    static final String ANONYMIZED_FULL_NAME = "Usuário removido";
    static final String ANONYMIZED_EMAIL_DOMAIN = "@anonimizado.local";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAuditLogService userAuditLogService;

    @Transactional(readOnly = true)
    public PageResponseDTO<UserResponseDTO> findAll(Pageable pageable) {
        return PageResponseDTO.fromPage(userRepository.findAll(pageable).map(UserResponseDTO::fromEntity));
    }

    @Transactional(readOnly = true)
    public UserResponseDTO findById(Long userId) {
        User user = findEntityById(userId);
        requireAdminOrSelf(user);
        return UserResponseDTO.fromEntity(user);
    }

    @Transactional
    public UserResponseDTO create(UserCreateRequestDTO request) {
        UserPrincipal principal = currentPrincipal();
        if (!isAdmin(principal)) {
            throw new AccessDeniedException("Acesso negado: apenas administradores podem criar usuários");
        }

        String normalizedEmail = normalizeEmail(request.email());
        ensureEmailAvailable(normalizedEmail, null);

        User newUser = User.builder()
                .fullName(request.fullName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .active(true)
                .build();
        User createdUser = userRepository.save(newUser);

        userAuditLogService.recordByCurrentUser(
                UserAuditAction.USER_CREATED,
                createdUser.getId(),
                createdUser.getEmail(),
                "Conta criada pelo administrador " + principal.getEmail());
        return UserResponseDTO.fromEntity(createdUser);
    }

    @Transactional
    public UserResponseDTO update(Long userId, UserUpdateRequestDTO request) {
        User user = findEntityById(userId);
        UserPrincipal principal = requireAdminOrSelf(user);

        List<String> changes = new ArrayList<>();

        String normalizedEmail = normalizeEmail(request.email());
        if (!normalizedEmail.equals(user.getEmail())) {
            ensureEmailAvailable(normalizedEmail, user.getId());
            changes.add("email: '" + user.getEmail() + "' -> '" + normalizedEmail + "'");
            user.setEmail(normalizedEmail);
        }

        String fullName = request.fullName().trim();
        if (!fullName.equals(user.getFullName())) {
            changes.add("fullName: '" + user.getFullName() + "' -> '" + fullName + "'");
            user.setFullName(fullName);
        }

        if (request.role() != null && request.role() != user.getRole()) {
            requireAdmin(principal, "perfil");
            changes.add("role: '" + user.getRole() + "' -> '" + request.role() + "'");
            user.setRole(request.role());
        }

        if (request.active() != null && request.active() != user.isActive()) {
            requireAdmin(principal, "situação ativa");
            changes.add("active: '" + user.isActive() + "' -> '" + request.active() + "'");
            user.setActive(request.active());
        }

        if (changes.isEmpty()) {
            return UserResponseDTO.fromEntity(user);
        }

        User updatedUser = userRepository.save(user);
        userAuditLogService.recordByCurrentUser(
                UserAuditAction.USER_UPDATED,
                updatedUser.getId(),
                updatedUser.getEmail(),
                String.join("; ", changes));
        return UserResponseDTO.fromEntity(updatedUser);
    }

    @Transactional
    public void anonymize(Long userId) {
        User user = findEntityById(userId);
        UserPrincipal principal = requireAdminOrSelf(user);

        if (user.getRole() == Role.ADMIN && user.isActive()
                && userRepository.countByRoleAndActiveTrueAndIdNot(Role.ADMIN, user.getId()) == 0) {
            throw new RoleNotAllowedException(
                    "Operação não permitida: é necessário manter ao menos um administrador ativo");
        }

        String originalEmail = user.getEmail();
        user.setFullName(ANONYMIZED_FULL_NAME);
        user.setEmail(anonymizedEmailFor(user.getId()));
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        user.setActive(false);
        userRepository.save(user);

        userAuditLogService.recordByCurrentUser(
                UserAuditAction.USER_ANONYMIZED,
                user.getId(),
                originalEmail,
                "Conta anonimizada pelo usuário " + principal.getEmail()
                        + "; e-mail original preservado para rastreabilidade: " + originalEmail);
    }

    static String anonymizedEmailFor(Long userId) {
        return "anonimizado-" + userId + ANONYMIZED_EMAIL_DOMAIN;
    }

    private User findEntityById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + userId));
    }

    private UserPrincipal requireAdminOrSelf(User target) {
        UserPrincipal principal = currentPrincipal();
        if (!isAdmin(principal) && !Objects.equals(principal.getId(), target.getId())) {
            throw new AccessDeniedException("Acesso negado: você só pode acessar a própria conta");
        }
        return principal;
    }

    private void requireAdmin(UserPrincipal principal, String fieldDescription) {
        if (!isAdmin(principal)) {
            throw new AccessDeniedException(
                    "Acesso negado: apenas administradores podem alterar " + fieldDescription);
        }
    }

    private boolean isAdmin(UserPrincipal principal) {
        return principal != null && Role.ADMIN.name().equals(principal.getRole());
    }

    private UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new InvalidCredentialsException();
        }
        return principal;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void ensureEmailAvailable(String normalizedEmail, Long excludingUserId) {
        userRepository.findByEmail(normalizedEmail)
                .filter(existing -> !Objects.equals(existing.getId(), excludingUserId))
                .ifPresent(existing -> {
                    throw new EmailAlreadyExistsException(normalizedEmail);
                });
    }
}
