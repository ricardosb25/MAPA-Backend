package com.mapa.security;

import com.mapa.domain.User;
import com.mapa.domain.enums.Role;
import com.mapa.dto.auth.LoginRequestDTO;
import com.mapa.dto.user.UserUpdateRequestDTO;
import com.mapa.repository.UserRepository;
import com.mapa.service.AuthService;
import com.mapa.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SessionSecurityIntegrationTest {
    private static final String CURRENT_PASSWORD = "CurrentPassword123!";
    private static final String NEW_PASSWORD = "NewPassword123!";
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private AuthService authService;
    @Autowired private UserService userService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldDenyDisabledAccountsEvenWithCorrectlySignedToken() throws Exception {
        User account = createAccount(Role.STUDENT);
        String token = jwtService.generateToken(account);
        account.setActive(false);
        userRepository.saveAndFlush(account);
        expectTokenStatus(token, 401);
    }

    @Test
    void shouldRevokeOldTokenAfterPasswordChangeAndAcceptNewLogin() throws Exception {
        User account = createAccount(Role.STUDENT);
        String token = jwtService.generateToken(account);
        mockMvc.perform(put("/api/v1/account/settings/password")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + CURRENT_PASSWORD
                                + "\",\"newPassword\":\"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isOk());
        expectTokenStatus(token, 401);
        expectTokenStatus(authService.login(new LoginRequestDTO(account.getEmail(), NEW_PASSWORD)).token(), 200);
        assertEquals(1, account.getCredentialsVersion());
    }

    @Test
    void shouldRevokeOldTokenAfterPasswordRecovery() throws Exception {
        User account = createAccount(Role.TEACHER);
        account.setResetToken(UUID.randomUUID().toString());
        account.setResetTokenExpiresAt(OffsetDateTime.now().plusMinutes(30));
        userRepository.saveAndFlush(account);
        String token = jwtService.generateToken(account);
        mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + account.getResetToken()
                                + "\",\"password\":\"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isOk());
        expectTokenStatus(token, 401);
        expectTokenStatus(authService.login(new LoginRequestDTO(account.getEmail(), NEW_PASSWORD)).token(), 200);
    }

    @Test
    void shouldNotReviveOldTokensWhenAnAccountIsReactivated() throws Exception {
        User account = createAccount(Role.STUDENT);
        User admin = createAccount(Role.ADMIN);
        String token = jwtService.generateToken(account);
        authenticate(admin);
        userService.update(account.getId(), new UserUpdateRequestDTO(account.getFullName(), account.getEmail(), null, false));
        userService.update(account.getId(), new UserUpdateRequestDTO(account.getFullName(), account.getEmail(), null, true));
        userRepository.flush();
        SecurityContextHolder.clearContext();
        expectTokenStatus(token, 401);
        expectTokenStatus(authService.login(new LoginRequestDTO(account.getEmail(), CURRENT_PASSWORD)).token(), 200);
    }

    @Test
    void shouldNotAuthenticateAnotherAccountThatReusesTheOldEmail() throws Exception {
        User account = createAccount(Role.STUDENT);
        String previousEmail = account.getEmail();
        String token = jwtService.generateToken(account);
        mockMvc.perform(put("/api/v1/account/settings/email").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new-" + previousEmail + "\",\"currentPassword\":\"" + CURRENT_PASSWORD + "\"}"))
                .andExpect(status().isOk());
        User otherAccount = createAccount(Role.STUDENT);
        otherAccount.setEmail(previousEmail);
        userRepository.saveAndFlush(otherAccount);
        expectTokenStatus(token, 401);
        expectTokenStatus(authService.login(new LoginRequestDTO(account.getEmail(), CURRENT_PASSWORD)).token(), 200);
    }

    @Test
    void shouldRejectDemotionOfLastAdminThroughApi() throws Exception {
        User admin = userRepository.findAll().stream()
                .filter(account -> account.isActive() && account.getRole() == Role.ADMIN)
                .findFirst().orElseThrow();
        mockMvc.perform(put("/api/v1/users/" + admin.getId())
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"" + admin.getFullName() + "\",\"email\":\""
                                + admin.getEmail() + "\",\"role\":\"STUDENT\",\"active\":false}"))
                .andExpect(status().isForbidden());
        assertEquals(Role.ADMIN, admin.getRole());
    }

    @Test
    void shouldRejectStaleWritesThatCouldRestorePreviousCredentials() {
        User account = createAccount(Role.STUDENT);
        jdbcTemplate.update("UPDATE users SET password_hash = ?, credentials_version = credentials_version + 1, "
                        + "row_version = row_version + 1 WHERE id = ?",
                passwordEncoder.encode(NEW_PASSWORD), account.getId());
        account.setFullName("Perfil desatualizado");
        assertThrows(OptimisticLockingFailureException.class, () -> userRepository.saveAndFlush(account));
    }

    private User createAccount(Role role) {
        return userRepository.saveAndFlush(User.builder().fullName("Conta de teste")
                .email("session-" + UUID.randomUUID() + "@mapa.test")
                .passwordHash(passwordEncoder.encode(CURRENT_PASSWORD)).role(role).active(true).build());
    }

    private void authenticate(User account) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UserPrincipal(account), null, List.of()));
    }

    private void expectTokenStatus(String token, int expectedStatus) throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().is(expectedStatus));
    }
}
