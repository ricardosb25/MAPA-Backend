package com.mapa.service;

import com.mapa.domain.enums.Role;
import com.mapa.dto.auth.RegisterRequestDTO;
import com.mapa.exception.RoleNotAllowedException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldBlockAdminRegistration() {
        AuthService authService = new AuthService(null, null, null, null);
        RegisterRequestDTO adminRequest = new RegisterRequestDTO(
                "Administrador", "admin@email.com", "senha123456", Role.ADMIN);

        assertThrows(RoleNotAllowedException.class, () -> authService.register(adminRequest));
    }

    @Test
    void shouldValidateRequiredRegistrationFields() {
        RegisterRequestDTO invalidRequest = new RegisterRequestDTO("", "email-invalido", "curta", null);

        Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(invalidRequest);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldAcceptValidStudentAndTeacherRegistration() {
        RegisterRequestDTO studentRequest =
                new RegisterRequestDTO("Ana Silva", "ana@email.com", "senha123456", Role.STUDENT);
        RegisterRequestDTO teacherRequest =
                new RegisterRequestDTO("Carlos Souza", "carlos@email.com", "senha123456", Role.TEACHER);

        assertTrue(validator.validate(studentRequest).isEmpty());
        assertTrue(validator.validate(teacherRequest).isEmpty());
    }
}

