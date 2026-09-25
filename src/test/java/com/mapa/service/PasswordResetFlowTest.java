package com.mapa.service;

import com.mapa.domain.User;
import com.mapa.domain.enums.Role;
import com.mapa.dto.auth.ForgotPasswordRequestDTO;
import com.mapa.dto.auth.LoginRequestDTO;
import com.mapa.dto.auth.MessageResponseDTO;
import com.mapa.dto.auth.ResetPasswordRequestDTO;
import com.mapa.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PasswordResetFlowTest {

    private static final String CANDIDATE_EMAIL = "reset.candidate@email.com";
    private static final String EXPIRED_USER_EMAIL = "reset.expired@email.com";
    private static final String EXPIRED_TOKEN = "token-expirado-para-teste";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailDeliveryStatus emailDeliveryStatus;

    @AfterEach
    void cleanUpCreatedUsers() {
        userRepository.findByEmail(CANDIDATE_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmail(EXPIRED_USER_EMAIL).ifPresent(userRepository::delete);
        emailDeliveryStatus.clear();
    }

    @Test
    void shouldReturnGenericSuccessMessageForUnknownEmail() {
        ResponseEntity<MessageResponseDTO> response = postForgotPassword("nao.cadastrado@email.com");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().message());
    }

    @Test
    void shouldIssueResetTokenAndAllowPasswordChangeOnlyOnce() {
        createCandidateUser();

        assertEquals(HttpStatus.OK, postForgotPassword(CANDIDATE_EMAIL).getStatusCode());

        User issuedUser = userRepository.findByEmail(CANDIDATE_EMAIL).orElseThrow();
        assertNotNull(issuedUser.getResetToken());
        assertNotNull(issuedUser.getResetTokenExpiresAt());
        String issuedToken = issuedUser.getResetToken();

        assertEquals(HttpStatus.OK, postResetPassword(issuedToken, "senhaNova12345"));

        User redefinedUser = userRepository.findByEmail(CANDIDATE_EMAIL).orElseThrow();
        assertNull(redefinedUser.getResetToken());
        assertNull(redefinedUser.getResetTokenExpiresAt());

        assertEquals(HttpStatus.OK, loginStatus(CANDIDATE_EMAIL, "senhaNova12345"));
        assertEquals(HttpStatus.UNAUTHORIZED, loginStatus(CANDIDATE_EMAIL, "senhaAntiga123"));
        assertEquals(HttpStatus.BAD_REQUEST, postResetPassword(issuedToken, "outraSenha12345"));
    }

    @Test
    void shouldRejectResetWithUnknownToken() {
        assertEquals(HttpStatus.BAD_REQUEST, postResetPassword("token-inexistente", "senhaNova12345"));
    }

    @Test
    void shouldRejectResetWithExpiredToken() {
        createExpiredTokenUser();

        assertEquals(HttpStatus.BAD_REQUEST, postResetPassword(EXPIRED_TOKEN, "senhaNova12345"));
    }

    @Test
    void shouldReturnServiceUnavailableForBothKnownAndUnknownEmailsWhenEmailDeliveryIsFailing() {
        createCandidateUser();
        emailDeliveryStatus.markFailure();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, forgotPasswordStatus("nao.cadastrado@email.com"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, forgotPasswordStatus(CANDIDATE_EMAIL));
    }

    private void createCandidateUser() {
        userRepository.save(User.builder()
                .fullName("Ana Reset")
                .email(CANDIDATE_EMAIL)
                .passwordHash(passwordEncoder.encode("senhaAntiga123"))
                .role(Role.STUDENT)
                .active(true)
                .build());
    }

    private void createExpiredTokenUser() {
        userRepository.save(User.builder()
                .fullName("Token Expirado")
                .email(EXPIRED_USER_EMAIL)
                .passwordHash(passwordEncoder.encode("senhaAntiga123"))
                .role(Role.STUDENT)
                .active(true)
                .resetToken(EXPIRED_TOKEN)
                .resetTokenExpiresAt(OffsetDateTime.now().minusMinutes(1))
                .build());
    }

    private ResponseEntity<MessageResponseDTO> postForgotPassword(String email) {
        return restClient().post().uri("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ForgotPasswordRequestDTO(email))
                .retrieve().toEntity(MessageResponseDTO.class);
    }

    private HttpStatusCode forgotPasswordStatus(String email) {
        try {
            return restClient().post().uri("/api/v1/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ForgotPasswordRequestDTO(email))
                    .retrieve().toBodilessEntity().getStatusCode();
        } catch (RestClientResponseException exception) {
            return exception.getStatusCode();
        }
    }

    private HttpStatusCode postResetPassword(String token, String password) {
        try {
            return restClient().post().uri("/api/v1/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ResetPasswordRequestDTO(token, password))
                    .retrieve().toBodilessEntity().getStatusCode();
        } catch (RestClientResponseException exception) {
            return exception.getStatusCode();
        }
    }

    private HttpStatusCode loginStatus(String email, String password) {
        try {
            return restClient().post().uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new LoginRequestDTO(email, password))
                    .retrieve().toBodilessEntity().getStatusCode();
        } catch (RestClientResponseException exception) {
            return exception.getStatusCode();
        }
    }

    private RestClient restClient() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }
}