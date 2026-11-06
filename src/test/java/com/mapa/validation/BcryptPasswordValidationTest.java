package com.mapa.validation;

import com.mapa.dto.auth.RegisterRequestDTO;
import com.mapa.dto.auth.ResetPasswordRequestDTO;
import com.mapa.dto.user.PasswordChangeRequestDTO;
import com.mapa.dto.user.UserCreateRequestDTO;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BcryptPasswordValidationTest {
    @Test
    void enforcesUtf8ByteLimitOnAllNewPasswordFields() {
        String exactLimit = "é".repeat(36);
        String aboveLimit = exactLimit + "a";

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validateValue(RegisterRequestDTO.class, "password", exactLimit).isEmpty());
            assertEquals(1, validator.validateValue(RegisterRequestDTO.class, "password", aboveLimit).size());
            assertEquals(1, validator.validateValue(ResetPasswordRequestDTO.class, "password", aboveLimit).size());
            assertEquals(1, validator.validateValue(UserCreateRequestDTO.class, "password", aboveLimit).size());
            assertEquals(1, validator.validateValue(PasswordChangeRequestDTO.class, "newPassword", aboveLimit).size());
        }
    }
}
