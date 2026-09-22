package com.mapa.controller;

import com.mapa.domain.enums.Role;
import com.mapa.dto.auth.AuthResponseDTO;
import com.mapa.dto.auth.LoginRequestDTO;
import com.mapa.dto.auth.RegisterRequestDTO;
import com.mapa.dto.auth.UserResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthControllerTest {

    @LocalServerPort
    private int port;

    private RestClient restClient() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    private HttpStatusCode registerStatus(String email, Role role) {
        try {
            return restClient().post().uri("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RegisterRequestDTO("Ana Silva", email, "password123", role))
                    .retrieve().toBodilessEntity().getStatusCode();
        } catch (RestClientResponseException exception) {
            return exception.getStatusCode();
        }
    }

    private HttpStatusCode registerRawStatus(String rawBody) {
        try {
            return restClient().post().uri("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(rawBody)
                    .retrieve().toBodilessEntity().getStatusCode();
        } catch (RestClientResponseException exception) {
            return exception.getStatusCode();
        }
    }

    private HttpStatusCode getMeStatus(String bearerToken) {
        try {
            RestClient.RequestHeadersSpec<?> request = restClient().get().uri("/api/v1/auth/me");
            if (bearerToken != null) {
                request = request.header("Authorization", "Bearer " + bearerToken);
            }
            return request.retrieve().toBodilessEntity().getStatusCode();
        } catch (RestClientResponseException exception) {
            return exception.getStatusCode();
        }
    }

    @Test
    void shouldRegisterAndLoginAndFetchCurrentUser() {
        ResponseEntity<UserResponseDTO> registerResponse = restClient().post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequestDTO("Ana Silva", "ana@email.com", "password123", Role.STUDENT))
                .retrieve().toEntity(UserResponseDTO.class);
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        assertNotNull(registerResponse.getBody());

        ResponseEntity<AuthResponseDTO> loginResponse = restClient().post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequestDTO("ana@email.com", "password123"))
                .retrieve().toEntity(AuthResponseDTO.class);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertNotNull(loginResponse.getBody().token());

        ResponseEntity<UserResponseDTO> meResponse = restClient().get().uri("/api/v1/auth/me")
                .header("Authorization", "Bearer " + loginResponse.getBody().token())
                .retrieve().toEntity(UserResponseDTO.class);
        assertEquals(HttpStatus.OK, meResponse.getStatusCode());
        assertEquals("ana@email.com", meResponse.getBody().email());
    }

    @Test
    void shouldRejectCurrentUserWithoutToken() {
        assertEquals(HttpStatus.UNAUTHORIZED, getMeStatus(null));
    }

    @Test
    void shouldRejectCurrentUserWithInvalidToken() {
        assertEquals(HttpStatus.UNAUTHORIZED, getMeStatus("invalid.token.here"));
    }

    @Test
    void shouldRejectDuplicateRegistration() {
        assertEquals(HttpStatus.CREATED, registerStatus("duplicate@email.com", Role.STUDENT));
        assertEquals(HttpStatus.CONFLICT, registerStatus("duplicate@email.com", Role.TEACHER));
    }

    @Test
    void shouldRejectAdminRegistration() {
        assertEquals(HttpStatus.FORBIDDEN, registerStatus("admin@email.com", Role.ADMIN));
    }

    @Test
    void shouldRejectInvalidRegistrationPayload() {
        assertEquals(HttpStatus.BAD_REQUEST, registerRawStatus(
                "{\"fullName\":\"\",\"email\":\"invalid\",\"password\":\"short\",\"role\":null}"));
    }
}

