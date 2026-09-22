package com.mapa.service;

import com.mapa.domain.User;
import com.mapa.domain.enums.Role;
import com.mapa.dto.auth.AuthResponseDTO;
import com.mapa.dto.auth.LoginRequestDTO;
import com.mapa.dto.auth.RegisterRequestDTO;
import com.mapa.dto.auth.UserResponseDTO;
import com.mapa.exception.EmailAlreadyExistsException;
import com.mapa.exception.InvalidCredentialsException;
import com.mapa.exception.RoleNotAllowedException;
import com.mapa.repository.UserRepository;
import com.mapa.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RegisterRequestDTO studentRequest(String email) {
        return new RegisterRequestDTO("Ana Silva", email, "password123", Role.STUDENT);
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@email.com";
    }

    @Test
    void shouldRegisterStudentAndLogin() {
        String email = uniqueEmail();
        UserResponseDTO createdUser = authService.register(studentRequest(email));

        assertNotNull(createdUser.id());
        assertEquals(Role.STUDENT, createdUser.role());
        assertTrue(userRepository.existsByEmail(email));
        assertTrue(passwordEncoder.matches("password123",
                userRepository.findByEmail(email).orElseThrow().getPasswordHash()));

        AuthResponseDTO loginResponse = authService.login(new LoginRequestDTO(email, "password123"));

        assertNotNull(loginResponse.token());
        assertEquals("Bearer", loginResponse.type());
        assertEquals(email, loginResponse.user().email());
    }

    @Test
    void shouldRejectLoginWithWrongPassword() {
        String email = uniqueEmail();
        authService.register(studentRequest(email));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequestDTO(email, "wrong-password")));
    }

    @Test
    void shouldRejectLoginForInactiveUser() {
        String email = uniqueEmail();
        UserResponseDTO createdUser = authService.register(studentRequest(email));
        User storedUser = userRepository.findById(createdUser.id()).orElseThrow();
        storedUser.setActive(false);
        userRepository.save(storedUser);

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequestDTO(email, "password123")));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        String email = uniqueEmail();
        authService.register(studentRequest(email));

        assertThrows(EmailAlreadyExistsException.class,
                () -> authService.register(studentRequest(email.toUpperCase())));
    }

    @Test
    void shouldRejectAdminRegistration() {
        RegisterRequestDTO adminRequest =
                new RegisterRequestDTO("Admin", uniqueEmail(), "password123", Role.ADMIN);

        assertThrows(RoleNotAllowedException.class, () -> authService.register(adminRequest));
    }

    @Test
    void shouldReturnCurrentUser() {
        String email = uniqueEmail();
        UserResponseDTO createdUser = authService.register(studentRequest(email));
        User storedUser = userRepository.findById(createdUser.id()).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(storedUser), null,
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));

        try {
            UserResponseDTO currentUser = authService.getCurrentUser();
            assertEquals(email, currentUser.email());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
