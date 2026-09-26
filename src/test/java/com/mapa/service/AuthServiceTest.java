package com.mapa.service;

import com.mapa.domain.enums.Role;
import com.mapa.dto.auth.ForgotPasswordRequestDTO;
import com.mapa.dto.auth.RegisterRequestDTO;
import com.mapa.dto.auth.ResetPasswordRequestDTO;
import com.mapa.exception.EmailDeliveryException;
import com.mapa.exception.RoleNotAllowedException;
import com.mapa.exception.TermsNotAcceptedException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldBlockAdminRegistration() {
        AuthService authService = new AuthService(null, null, null, null, null, null, null, null);
        RegisterRequestDTO adminRequest = new RegisterRequestDTO(
                "Administrador", "admin@email.com", "senha123456", Role.ADMIN, true);

        assertThrows(RoleNotAllowedException.class, () -> authService.register(adminRequest));
    }

    @Test
    void shouldFailFastWithoutTouchingRepositoriesWhenEmailServiceIsUnavailable() {
        EmailDeliveryStatus emailDeliveryStatus = new EmailDeliveryStatus(
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
        emailDeliveryStatus.markFailure();
        AuthService authService = new AuthService(null, null, null, null, null, null, emailDeliveryStatus, null);

        assertThrows(EmailDeliveryException.class,
                () -> authService.forgotPassword(new ForgotPasswordRequestDTO("ana@email.com")));
    }

    @Test
    void shouldValidateRequiredRegistrationFields() {
        RegisterRequestDTO invalidRequest = new RegisterRequestDTO("", "email-invalido", "curta", null, true);

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(invalidRequest);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldRequireTermsAcceptanceInBeanValidation() {
        RegisterRequestDTO missingTerms = new RegisterRequestDTO(
                "Ana Silva", "ana@email.com", "senha123456", Role.STUDENT, null);

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(missingTerms);

        assertTrue(violations.stream()
                .anyMatch(violation -> "acceptedTerms".equals(violation.getPropertyPath().toString())));
    }

    @Test
    void shouldRejectRegistrationWhenTermsAreNotAccepted() {
        AuthService authService = new AuthService(null, null, null, null, null, null, null, null);
        RegisterRequestDTO refusedTerms = new RegisterRequestDTO(
                "Ana Silva", "ana@email.com", "senha123456", Role.STUDENT, false);

        assertThrows(TermsNotAcceptedException.class, () -> authService.register(refusedTerms));
    }

    @Test
    void shouldAcceptValidStudentAndTeacherRegistration() {
        RegisterRequestDTO studentRequest =
                new RegisterRequestDTO("Ana Silva", "ana@email.com", "senha123456", Role.STUDENT, true);
        RegisterRequestDTO teacherRequest =
                new RegisterRequestDTO("Carlos Souza", "carlos@email.com", "senha123456", Role.TEACHER, true);

        assertTrue(validator.validate(studentRequest).isEmpty());
        assertTrue(validator.validate(teacherRequest).isEmpty());
    }

    @Test
    void shouldValidateForgotPasswordRequestFields() {
        Set<ConstraintViolation<ForgotPasswordRequestDTO>> blankEmailViolations =
                validator.validate(new ForgotPasswordRequestDTO(""));
        Set<ConstraintViolation<ForgotPasswordRequestDTO>> malformedEmailViolations =
                validator.validate(new ForgotPasswordRequestDTO("email-invalido"));

        assertFalse(blankEmailViolations.isEmpty());
        assertFalse(malformedEmailViolations.isEmpty());
        assertTrue(validator.validate(new ForgotPasswordRequestDTO("ana@email.com")).isEmpty());
    }

    @Test
    void shouldValidateResetPasswordRequestFields() {
        Set<ConstraintViolation<ResetPasswordRequestDTO>> missingTokenViolations =
                validator.validate(new ResetPasswordRequestDTO("", "senha123456"));
        Set<ConstraintViolation<ResetPasswordRequestDTO>> shortPasswordViolations =
                validator.validate(new ResetPasswordRequestDTO("token-valido", "curta"));

        assertFalse(missingTokenViolations.isEmpty());
        assertFalse(shortPasswordViolations.isEmpty());
        assertTrue(validator.validate(new ResetPasswordRequestDTO("token-valido", "senha123456")).isEmpty());
    }
}

