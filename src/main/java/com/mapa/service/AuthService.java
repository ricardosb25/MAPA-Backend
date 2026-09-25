package com.mapa.service;

import com.mapa.domain.User;
import com.mapa.domain.enums.Role;
import com.mapa.dto.auth.AuthResponseDTO;
import com.mapa.dto.auth.ForgotPasswordRequestDTO;
import com.mapa.dto.auth.LoginRequestDTO;
import com.mapa.dto.auth.MessageResponseDTO;
import com.mapa.dto.auth.RegisterRequestDTO;
import com.mapa.dto.auth.ResetPasswordRequestDTO;
import com.mapa.dto.auth.UserResponseDTO;
import com.mapa.exception.EmailDeliveryException;
import com.mapa.exception.InvalidCredentialsException;
import com.mapa.exception.EmailAlreadyExistsException;
import com.mapa.exception.InvalidResetTokenException;
import com.mapa.exception.RoleNotAllowedException;
import com.mapa.exception.ResourceNotFoundException;
import com.mapa.config.properties.FrontendProperties;
import com.mapa.config.properties.JwtProperties;
import com.mapa.repository.UserRepository;
import com.mapa.security.JwtService;
import com.mapa.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration RESET_TOKEN_EXPIRATION = Duration.ofMinutes(30);
    private static final String FORGOT_PASSWORD_MESSAGE =
            "Se o e-mail informado estiver cadastrado, enviaremos um link de redefinição em instantes.";
    private static final String RESET_PASSWORD_MESSAGE = "Senha redefinida com sucesso.";
    private static final String EMAIL_SERVICE_UNAVAILABLE_MESSAGE =
            "Serviço de e-mail indisponível no momento. Tente novamente em instantes ou entre em contato com o suporte.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final ResendEmailService resendEmailService;
    private final FrontendProperties frontendProperties;
    private final EmailDeliveryStatus emailDeliveryStatus;

    @Transactional
    public UserResponseDTO register(RegisterRequestDTO request) {
        if (request.role() == Role.ADMIN) {
            throw new RoleNotAllowedException("Cadastro com perfil ADMIN não é permitido");
        }
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }
        User newUser = User.builder()
                .fullName(request.fullName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .active(true)
                .build();
        return UserResponseDTO.fromEntity(userRepository.save(newUser));
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);
        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return AuthResponseDTO.bearer(token, jwtProperties.getExpirationMs() / 1000, UserResponseDTO.fromEntity(user));
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new InvalidCredentialsException();
        }
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        return UserResponseDTO.fromEntity(user);
    }

    @Transactional
    public MessageResponseDTO forgotPassword(ForgotPasswordRequestDTO request) {
        if (emailDeliveryStatus.isUnavailable()) {
            throw new EmailDeliveryException(EMAIL_SERVICE_UNAVAILABLE_MESSAGE);
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        userRepository.findByEmail(normalizedEmail)
                .filter(User::isActive)
                .ifPresent(this::issuePasswordResetLink);
        return MessageResponseDTO.of(FORGOT_PASSWORD_MESSAGE);
    }

    @Transactional
    public MessageResponseDTO resetPassword(ResetPasswordRequestDTO request) {
        User user = userRepository.findByResetToken(request.token())
                .filter(candidate -> candidate.getResetTokenExpiresAt() != null
                        && candidate.getResetTokenExpiresAt().isAfter(OffsetDateTime.now()))
                .orElseThrow(InvalidResetTokenException::new);

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        userRepository.save(user);
        return MessageResponseDTO.of(RESET_PASSWORD_MESSAGE);
    }

    private void issuePasswordResetLink(User user) {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String resetToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        user.setResetToken(resetToken);
        user.setResetTokenExpiresAt(OffsetDateTime.now().plus(RESET_TOKEN_EXPIRATION));
        userRepository.save(user);

        String resetUrl = frontendProperties.getBaseUrl() + "/reset-password?token=" + resetToken;
        resendEmailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetUrl);
    }
}

