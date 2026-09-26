package com.mapa.controller;

import com.mapa.domain.enums.Role;
import com.mapa.dto.MotorRequestDTO;
import com.mapa.dto.MotorResponseDTO;
import com.mapa.dto.auth.AuthResponseDTO;
import com.mapa.dto.auth.LoginRequestDTO;
import com.mapa.dto.auth.RegisterRequestDTO;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MotorControllerAuthorizationTest {

    private static final String ADMIN_EMAIL = "admin@mapa.test";
    private static final String ADMIN_PASSWORD = "AdminTest12345!";

    @LocalServerPort
    private int port;

    private RestClient restClient() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void shouldRejectMotorWritesWithoutToken() {
        MotorRequestDTO payload = motorPayload("Motor Sem Token");

        assertEquals(HttpStatus.UNAUTHORIZED, postMotorStatus(null, payload));
        assertEquals(HttpStatus.UNAUTHORIZED, putMotorStatus(null, 1L));
        assertEquals(HttpStatus.UNAUTHORIZED, deleteMotorStatus(null, 1L));
    }

    @Test
    void shouldAllowAuthenticatedNonAdminToReadMotors() {
        String studentToken = registerStudentAndGetToken();

        HttpStatusCode status = executeGet(studentToken, null).getStatusCode();

        assertEquals(HttpStatus.OK, status);
    }

    @Test
    void shouldRejectMotorCreationForNonAdmin() {
        String studentToken = registerStudentAndGetToken();

        HttpStatusCode status;
        try {
            status = executePost(studentToken, motorPayload("Motor Do Aluno")).getStatusCode();
        } catch (RestClientResponseException exception) {
            status = exception.getStatusCode();
            assertTrue(exception.getResponseBodyAsString().contains("Acesso negado"),
                    "Resposta 403 deve explicar a negação de acesso");
        }

        assertEquals(HttpStatus.FORBIDDEN, status);
    }

    @Test
    void shouldRejectMotorUpdateAndDeletionForNonAdmin() {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        Long motorId = adminCreatesMotor(adminToken, "Motor Protegido " + uniqueSuffix());
        String studentToken = registerStudentAndGetToken();

        assertEquals(HttpStatus.FORBIDDEN, putMotorStatus(studentToken, motorId));
        assertEquals(HttpStatus.FORBIDDEN, deleteMotorStatus(studentToken, motorId));
        assertEquals(HttpStatus.OK, executeGet(studentToken, motorId).getStatusCode());
    }

    @Test
    void shouldAllowAdminToCreateUpdateAndDeleteMotor() {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        ResponseEntity<MotorResponseDTO> createResponse =
                executePost(adminToken, motorPayload("Motor Admin " + uniqueSuffix()));
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        Long motorId = createResponse.getBody().id();

        assertEquals(HttpStatus.OK, putMotorStatus(adminToken, motorId));
        assertEquals(HttpStatus.NO_CONTENT, deleteMotorStatus(adminToken, motorId));
    }

    private String registerStudentAndGetToken() {
        String studentEmail = "student-" + UUID.randomUUID() + "@email.com";
        ResponseEntity<?> registerResponse = restClient().post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequestDTO("Aluno Teste", studentEmail, "password123", Role.STUDENT, true))
                .retrieve().toEntity(Object.class);
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());

        return login(studentEmail, "password123");
    }

    private String login(String email, String password) {
        AuthResponseDTO authResponse = restClient().post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequestDTO(email, password))
                .retrieve().body(AuthResponseDTO.class);

        assertNotNull(authResponse);
        return authResponse.token();
    }

    private Long adminCreatesMotor(String adminToken, String motorName) {
        ResponseEntity<MotorResponseDTO> createResponse = executePost(adminToken, motorPayload(motorName));
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        return createResponse.getBody().id();
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private MotorRequestDTO motorPayload(String motorName) {
        return new MotorRequestDTO(
                "Honda",
                motorName,
                "Motor de teste para validação de autorização",
                new BigDecimal("0.2"),
                160,
                new BigDecimal("9.5"),
                8500,
                "ASPIRADO");
    }

    private HttpStatusCode postMotorStatus(String bearerToken, MotorRequestDTO payload) {
        try {
            return executePost(bearerToken, payload).getStatusCode();
        } catch (RestClientResponseException exception) {
            return exception.getStatusCode();
        }
    }

    private HttpStatusCode putMotorStatus(String bearerToken, Long motorId) {
        try {
            return executePut(bearerToken, motorId).getStatusCode();
        } catch (RestClientResponseException exception) {
            return exception.getStatusCode();
        }
    }

    private HttpStatusCode deleteMotorStatus(String bearerToken, Long motorId) {
        try {
            return executeDelete(bearerToken, motorId).getStatusCode();
        } catch (RestClientResponseException exception) {
            return exception.getStatusCode();
        }
    }

    private ResponseEntity<MotorResponseDTO> executePost(String bearerToken, MotorRequestDTO payload) {
        RestClient.RequestHeadersSpec<?> request = restClient().post().uri("/api/v1/motores")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload);
        return withBearer(request, bearerToken).retrieve().toEntity(MotorResponseDTO.class);
    }

    private ResponseEntity<MotorResponseDTO> executePut(String bearerToken, Long motorId) {
        RestClient.RequestHeadersSpec<?> request = restClient().put().uri("/api/v1/motores/{engineId}", motorId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(motorPayload("Motor Atualizado " + uniqueSuffix()));
        return withBearer(request, bearerToken).retrieve().toEntity(MotorResponseDTO.class);
    }

    private ResponseEntity<Void> executeDelete(String bearerToken, Long motorId) {
        RestClient.RequestHeadersSpec<?> request = restClient().delete().uri("/api/v1/motores/{engineId}", motorId);
        return withBearer(request, bearerToken).retrieve().toBodilessEntity();
    }

    private ResponseEntity<MotorResponseDTO> executeGet(String bearerToken, Long motorId) {
        RestClient.RequestHeadersSpec<?> request = motorId == null
                ? restClient().get().uri("/api/v1/motores")
                : restClient().get().uri("/api/v1/motores/{engineId}", motorId);
        return withBearer(request, bearerToken).retrieve().toEntity(MotorResponseDTO.class);
    }

    private RestClient.RequestHeadersSpec<?> withBearer(RestClient.RequestHeadersSpec<?> request, String bearerToken) {
        if (bearerToken == null) {
            return request;
        }
        return request.header("Authorization", "Bearer " + bearerToken);
    }
}
