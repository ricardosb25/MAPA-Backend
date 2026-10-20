package com.mapa.controller;

import com.mapa.domain.enums.ChallengeDifficulty;
import com.mapa.domain.enums.EngineStatus;
import com.mapa.domain.enums.Role;
import com.mapa.domain.enums.Shift;
import com.mapa.dto.MotorRequestDTO;
import com.mapa.dto.MotorResponseDTO;
import com.mapa.dto.PageResponseDTO;
import com.mapa.dto.auth.AuthResponseDTO;
import com.mapa.dto.auth.LoginRequestDTO;
import com.mapa.dto.auth.RegisterRequestDTO;
import com.mapa.dto.challenge.ChallengePerformanceDTO;
import com.mapa.dto.challenge.ChallengeRequestDTO;
import com.mapa.dto.challenge.ChallengeResponseDTO;
import com.mapa.dto.schoolClass.ClassJoinRequestDTO;
import com.mapa.dto.schoolClass.ClassRequestDTO;
import com.mapa.dto.schoolClass.ClassResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ChallengeControllerAuthorizationTest {

    private static final String PASSWORD = "password123";
    private static final String ADMIN_EMAIL = "admin@mapa.test";
    private static final String ADMIN_PASSWORD = "AdminTest12345!";

    @LocalServerPort
    private int port;

    @Test
    void shouldRejectChallengeEndpointsWithoutToken() {
        assertEquals(HttpStatus.UNAUTHORIZED, statusOf(() -> getTeacherChallenges(null)));
        assertEquals(HttpStatus.UNAUTHORIZED, statusOf(() -> getAvailableChallenges(null)));
        assertEquals(HttpStatus.UNAUTHORIZED, statusOf(() -> getActiveChallenge(null)));
    }

    @Test
    void shouldRestrictChallengeEndpointsByRole() {
        String teacherToken = registerAndGetToken("Professora Role", Role.TEACHER);
        String studentToken = registerAndGetToken("Aluno Role", Role.STUDENT);

        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> getTeacherChallenges(studentToken)));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> getAvailableChallenges(teacherToken)));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> getActiveChallenge(teacherToken)));
        assertEquals(HttpStatus.OK, statusOf(() -> getAvailableChallenges(studentToken)));
        assertEquals(HttpStatus.NO_CONTENT, statusOf(() -> getActiveChallenge(studentToken)));
    }

    @Test
    void shouldLetTeacherCreateAndStudentSelectAChallenge() {
        String teacherToken = registerAndGetToken("Professora Desafio", Role.TEACHER);
        String studentToken = registerAndGetToken("Aluno Desafio", Role.STUDENT);
        String otherTeacherToken = registerAndGetToken("Outro Professor", Role.TEACHER);
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        Long motorId = adminCreatesMotor(adminToken);
        ClassResponseDTO schoolClass = createClass(teacherToken, "Mecanica " + uniqueSuffix(), Shift.EVENING).getBody();
        assertNotNull(schoolClass);

        assertEquals(HttpStatus.OK, statusOf(() -> join(studentToken, schoolClass.code())));

        ChallengeRequestDTO payload = new ChallengeRequestDTO(
                "Falta de potência em alta rotação",
                "Corrija combustível e avanço sem provocar detonação.",
                ChallengeDifficulty.MEDIUM,
                motorId,
                schoolClass.id(),
                103,
                EngineStatus.INTEGRO);

        ChallengeResponseDTO createdChallenge = createChallenge(teacherToken, payload).getBody();
        assertNotNull(createdChallenge);
        assertEquals(103, createdChallenge.goalPowerCv());
        assertEquals(2, createdChallenge.criteria().size());
        assertFalse(createdChallenge.selected());

        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> getChallenge(otherTeacherToken, createdChallenge.id())));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> selectChallenge(otherTeacherToken, createdChallenge.id())));

        ChallengeResponseDTO selectedChallenge = selectChallenge(studentToken, createdChallenge.id()).getBody();
        assertNotNull(selectedChallenge);
        assertTrue(selectedChallenge.selected());

        ChallengeResponseDTO activeChallenge = getActiveChallenge(studentToken).getBody();
        assertNotNull(activeChallenge);
        assertEquals(createdChallenge.id(), activeChallenge.id());

        ChallengePerformanceDTO performance = getPerformance(teacherToken, createdChallenge.id()).getBody();
        assertNotNull(performance);
        assertEquals(1, performance.enrolledStudents());
        assertEquals(1, performance.students().size());
        assertTrue(performance.students().get(0).selected());

        assertEquals(HttpStatus.NO_CONTENT, statusOf(() -> deleteChallenge(teacherToken, createdChallenge.id())));
        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> getChallenge(teacherToken, createdChallenge.id())));
    }

    private RestClient restClient() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    private ResponseEntity<PageResponseDTO<ChallengeResponseDTO>> getTeacherChallenges(String token) {
        return executeGet(token, "/api/v1/challenges",
                new ParameterizedTypeReference<PageResponseDTO<ChallengeResponseDTO>>() { });
    }

    private ResponseEntity<PageResponseDTO<ChallengeResponseDTO>> getAvailableChallenges(String token) {
        return executeGet(token, "/api/v1/challenges/available",
                new ParameterizedTypeReference<PageResponseDTO<ChallengeResponseDTO>>() { });
    }

    private ResponseEntity<ChallengeResponseDTO> getActiveChallenge(String token) {
        return executeGet(token, "/api/v1/challenges/active",
                new ParameterizedTypeReference<ChallengeResponseDTO>() { });
    }

    private ResponseEntity<ChallengeResponseDTO> getChallenge(String token, Long challengeId) {
        return executeGet(token, "/api/v1/challenges/" + challengeId,
                new ParameterizedTypeReference<ChallengeResponseDTO>() { });
    }

    private ResponseEntity<ChallengePerformanceDTO> getPerformance(String token, Long challengeId) {
        return executeGet(token, "/api/v1/challenges/" + challengeId + "/performance",
                new ParameterizedTypeReference<ChallengePerformanceDTO>() { });
    }

    private <T> ResponseEntity<T> executeGet(String token, String uri, ParameterizedTypeReference<T> responseType) {
        RestClient.RequestHeadersSpec<?> request = restClient().get().uri(uri);
        return withBearer(request, token).retrieve().toEntity(responseType);
    }

    private ResponseEntity<ChallengeResponseDTO> createChallenge(String token, ChallengeRequestDTO payload) {
        RestClient.RequestHeadersSpec<?> request = restClient().post().uri("/api/v1/challenges")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload);
        return withBearer(request, token).retrieve().toEntity(ChallengeResponseDTO.class);
    }

    private ResponseEntity<ChallengeResponseDTO> selectChallenge(String token, Long challengeId) {
        RestClient.RequestHeadersSpec<?> request = restClient().post()
                .uri("/api/v1/challenges/" + challengeId + "/select");
        return withBearer(request, token).retrieve().toEntity(ChallengeResponseDTO.class);
    }

    private ResponseEntity<Void> deleteChallenge(String token, Long challengeId) {
        RestClient.RequestHeadersSpec<?> request = restClient().delete().uri("/api/v1/challenges/" + challengeId);
        return withBearer(request, token).retrieve().toBodilessEntity();
    }

    private ResponseEntity<ClassResponseDTO> createClass(String token, String name, Shift shift) {
        RestClient.RequestHeadersSpec<?> request = restClient().post().uri("/api/v1/classes")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ClassRequestDTO(name, shift));
        return withBearer(request, token).retrieve().toEntity(ClassResponseDTO.class);
    }

    private ResponseEntity<ClassResponseDTO> join(String token, String code) {
        RestClient.RequestHeadersSpec<?> request = restClient().post().uri("/api/v1/classes/join")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ClassJoinRequestDTO(code));
        return withBearer(request, token).retrieve().toEntity(ClassResponseDTO.class);
    }

    private Long adminCreatesMotor(String adminToken) {
        MotorRequestDTO payload = new MotorRequestDTO(
                "Volkswagen",
                "AP 1.8 8V " + UUID.randomUUID().toString().substring(0, 8),
                "Motor didático para testes de desafio",
                new BigDecimal("1.8"),
                1781,
                new BigDecimal("9.2"),
                6500,
                "ASPIRADO");

        ResponseEntity<MotorResponseDTO> createResponse = restClient().post().uri("/api/v1/motores")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .body(payload)
                .retrieve().toEntity(MotorResponseDTO.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        return createResponse.getBody().id();
    }

    private String registerAndGetToken(String fullName, Role role) {
        String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + "@email.com";
        ResponseEntity<?> registerResponse = restClient().post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequestDTO(fullName, email, PASSWORD, role, true))
                .retrieve().toEntity(Object.class);
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        return login(email, PASSWORD);
    }

    private String login(String email, String password) {
        AuthResponseDTO authResponse = restClient().post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequestDTO(email, password))
                .retrieve().body(AuthResponseDTO.class);
        assertNotNull(authResponse);
        return authResponse.token();
    }

    private HttpStatusCode statusOf(Supplier<ResponseEntity<?>> action) {
        try {
            return action.get().getStatusCode();
        } catch (RestClientResponseException exception) {
            return exception.getStatusCode();
        }
    }

    private RestClient.RequestHeadersSpec<?> withBearer(RestClient.RequestHeadersSpec<?> request, String bearerToken) {
        if (bearerToken == null) {
            return request;
        }
        return request.header("Authorization", "Bearer " + bearerToken);
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
