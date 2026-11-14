package com.mapa.service;

import com.mapa.domain.enums.ChallengeDifficulty;
import com.mapa.domain.enums.EngineStatus;
import com.mapa.domain.enums.Role;
import com.mapa.domain.enums.Shift;
import com.mapa.dto.DynoResultDTO;
import com.mapa.dto.MotorRequestDTO;
import com.mapa.dto.MotorResponseDTO;
import com.mapa.dto.MotorSelectionRequestDTO;
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
class ChallengeIntegrationTest {

    private static final String PASSWORD = "password123";
    private static final String ADMIN_EMAIL = "admin@mapa.test";
    private static final String ADMIN_PASSWORD = "AdminTest12345!";

    @LocalServerPort
    private int port;

    @Test
    void shouldCompleteChallengeWhenGoalAndEngineIntegrityAreMet() {
        String teacherToken = registerAndGetToken("Professora Carla", Role.TEACHER);
        String studentToken = registerAndGetToken("Aluno Pedro", Role.STUDENT);
        ClassResponseDTO schoolClass = createClass(teacherToken, "Turma E2E " + uniqueSuffix());
        joinClass(studentToken, schoolClass.code());
        Long motorId = createMotorId();
        selectMotor(studentToken, motorId);

        DynoResultDTO baseline = runDyno(studentToken);
        assertEquals(EngineStatus.INTEGRO, baseline.finalStatus());

        ChallengeResponseDTO challenge = createChallenge(teacherToken, schoolClass.id(), motorId,
                goalBelow(baseline.peakPower()), "Potência mínima E2E");

        assertEquals(2, challenge.criteria().size());
        assertFalse(challenge.selected());
        assertEquals(1, challenge.enrolledStudents());

        ChallengeResponseDTO selected = selectChallenge(studentToken, challenge.id());
        assertTrue(selected.selected());

        runDyno(studentToken);

        ChallengeResponseDTO active = getActiveChallenge(studentToken);
        assertNotNull(active);
        assertEquals(challenge.id(), active.id());
        assertTrue(active.selected());
        assertTrue(active.completed());
        assertEquals(2, active.completedCriteria());
        assertEquals(1, active.attempts());

        ChallengePerformanceDTO performance = getPerformance(teacherToken, challenge.id());
        assertEquals(1, performance.enrolledStudents());
        assertEquals(1, performance.completedStudents());
        assertEquals(1, performance.students().size());
        assertTrue(performance.students().get(0).completed());
        assertEquals(2, performance.students().get(0).completedCriteria());
        assertEquals(1, performance.students().get(0).attempts());

        PageResponseDTO<ChallengeResponseDTO> available = getAvailable(studentToken);
        ChallengeResponseDTO item = available.content().stream()
                .filter(candidate -> candidate.id().equals(challenge.id()))
                .findFirst()
                .orElseThrow();
        assertTrue(item.selected());
        assertTrue(item.completed());
    }

    @Test
    void shouldRecordAttemptWithoutCompletingWhenGoalIsNotReached() {
        String teacherToken = registerAndGetToken("Professora Sofia", Role.TEACHER);
        String studentToken = registerAndGetToken("Aluno Rafa", Role.STUDENT);
        ClassResponseDTO schoolClass = createClass(teacherToken, "Turma Meta " + uniqueSuffix());
        joinClass(studentToken, schoolClass.code());
        Long motorId = createMotorId();
        selectMotor(studentToken, motorId);

        ChallengeResponseDTO challenge = createChallenge(teacherToken, schoolClass.id(), motorId,
                99999, "Meta inalcançável E2E");
        selectChallenge(studentToken, challenge.id());
        runDyno(studentToken);

        ChallengeResponseDTO active = getActiveChallenge(studentToken);
        assertNotNull(active);
        assertFalse(active.completed());
        assertEquals(1, active.attempts());

        ChallengePerformanceDTO performance = getPerformance(teacherToken, challenge.id());
        assertEquals(1, performance.enrolledStudents());
        assertEquals(0, performance.completedStudents());
        assertEquals(1, performance.students().get(0).attempts());
        assertFalse(performance.students().get(0).completed());
    }

    @Test
    void shouldIgnoreRunsFromDifferentMotorThanChallenge() {
        String teacherToken = registerAndGetToken("Professora Dana", Role.TEACHER);
        String studentToken = registerAndGetToken("Aluno Igao", Role.STUDENT);
        ClassResponseDTO schoolClass = createClass(teacherToken, "Turma Motor " + uniqueSuffix());
        joinClass(studentToken, schoolClass.code());
        Long primaryMotorId = createMotorId();
        Long otherMotorId = createMotorId();
        selectMotor(studentToken, primaryMotorId);

        ChallengeResponseDTO otherMotorChallenge = createChallenge(teacherToken, schoolClass.id(), otherMotorId,
                100, "Motor divergente E2E");
        selectChallenge(studentToken, otherMotorChallenge.id());

        runDyno(studentToken);
        assertEquals(0, getPerformance(teacherToken, otherMotorChallenge.id())
                .students().get(0).attempts());

        selectMotor(studentToken, otherMotorId);
        runDyno(studentToken);

        ChallengePerformanceDTO performance = getPerformance(teacherToken, otherMotorChallenge.id());
        assertEquals(1, performance.students().get(0).attempts());
        assertNotNull(performance.students().get(0).lastPeakPower());
    }

    @Test
    void shouldKeepOnlyOneSelectedChallengeAndRecordAttemptsForIt() {
        String teacherToken = registerAndGetToken("Professora Beth", Role.TEACHER);
        String studentToken = registerAndGetToken("Aluno Luan", Role.STUDENT);
        ClassResponseDTO schoolClass = createClass(teacherToken, "Turma Seleção " + uniqueSuffix());
        joinClass(studentToken, schoolClass.code());
        Long motorId = createMotorId();
        selectMotor(studentToken, motorId);

        ChallengeResponseDTO first = createChallenge(teacherToken, schoolClass.id(), motorId,
                100, "Primeiro desafio E2E");
        ChallengeResponseDTO second = createChallenge(teacherToken, schoolClass.id(), motorId,
                100, "Segundo desafio E2E");

        selectChallenge(studentToken, first.id());
        assertEquals(first.id(), getActiveChallenge(studentToken).id());

        selectChallenge(studentToken, second.id());
        assertEquals(second.id(), getActiveChallenge(studentToken).id());

        PageResponseDTO<ChallengeResponseDTO> available = getAvailable(studentToken);
        assertFalse(available.content().stream()
                .filter(candidate -> candidate.id().equals(first.id()))
                .findFirst()
                .orElseThrow()
                .selected());
        assertTrue(available.content().stream()
                .filter(candidate -> candidate.id().equals(second.id()))
                .findFirst()
                .orElseThrow()
                .selected());

        runDyno(studentToken);

        assertEquals(0, getPerformance(teacherToken, first.id()).students().get(0).attempts());
        assertEquals(1, getPerformance(teacherToken, second.id()).students().get(0).attempts());
    }

    @Test
    void shouldRemoveChallengesWhenClassIsDeleted() {
        String teacherToken = registerAndGetToken("Professora Ceci", Role.TEACHER);
        String studentToken = registerAndGetToken("Aluno Vera", Role.STUDENT);
        ClassResponseDTO schoolClass = createClass(teacherToken, "Turma Delete " + uniqueSuffix());
        joinClass(studentToken, schoolClass.code());
        Long motorId = createMotorId();

        ChallengeResponseDTO challenge = createChallenge(teacherToken, schoolClass.id(), motorId,
                100, "Será removido E2E");
        selectChallenge(studentToken, challenge.id());
        assertTrue(getAvailable(studentToken).content().stream()
                .anyMatch(item -> item.id().equals(challenge.id())));

        assertEquals(HttpStatus.NO_CONTENT, deleteClass(teacherToken, schoolClass.id()).getStatusCode());

        assertTrue(getAvailable(studentToken).content().isEmpty());
        assertEquals(HttpStatus.NO_CONTENT, statusOf(() -> getActiveChallengeResponse(studentToken)));
        assertTrue(getChallengesByClass(teacherToken, schoolClass.id()).getBody().content().isEmpty());
    }

    private Integer goalBelow(Double achievedValue) {
        return (int) Math.max(1, achievedValue - 1.0);
    }

    private ChallengeResponseDTO createChallenge(
            String teacherToken, Long classId, Long motorId, Integer goalPowerCv, String title) {

        ChallengeRequestDTO request = new ChallengeRequestDTO(
                title + " " + uniqueSuffix(),
                "Desafio criado pelo teste de integração.",
                ChallengeDifficulty.MEDIUM,
                motorId,
                classId,
                goalPowerCv,
                EngineStatus.INTEGRO);

        ResponseEntity<ChallengeResponseDTO> response = restClient().post().uri("/api/v1/challenges")
                .header("Authorization", "Bearer " + teacherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve().toEntity(ChallengeResponseDTO.class);
        assertNotNull(response.getBody());
        return response.getBody();
    }

    private Long createMotorId() {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        ResponseEntity<MotorResponseDTO> motorResponse = restClient().post().uri("/api/v1/motores")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MotorRequestDTO(
                        "VW", "AP 1.8 E2E " + uniqueSuffix(), "Motor para integração de desafios",
                        new BigDecimal("1.8"), 1781, new BigDecimal("9.6"), 6800, "Aspirado"))
                .retrieve().toEntity(MotorResponseDTO.class);
        assertEquals(HttpStatus.CREATED, motorResponse.getStatusCode());
        assertNotNull(motorResponse.getBody());
        return motorResponse.getBody().id();
    }

    private void selectMotor(String token, Long motorId) {
        ResponseEntity<MotorResponseDTO> response = restClient().put().uri("/api/v1/motores/selecionado")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MotorSelectionRequestDTO(motorId))
                .retrieve().toEntity(MotorResponseDTO.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private ClassResponseDTO createClass(String token, String name) {
        ResponseEntity<ClassResponseDTO> classResponse = restClient().post().uri("/api/v1/classes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ClassRequestDTO(name, Shift.EVENING))
                .retrieve().toEntity(ClassResponseDTO.class);
        assertEquals(HttpStatus.CREATED, classResponse.getStatusCode());
        assertNotNull(classResponse.getBody());
        return classResponse.getBody();
    }

    private void joinClass(String studentToken, String classCode) {
        ResponseEntity<ClassResponseDTO> joinResponse = restClient().post().uri("/api/v1/classes/join")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ClassJoinRequestDTO(classCode))
                .retrieve().toEntity(ClassResponseDTO.class);
        assertEquals(HttpStatus.OK, joinResponse.getStatusCode());
    }

    private ResponseEntity<Void> deleteClass(String token, Long classId) {
        return restClient().delete().uri("/api/v1/classes/" + classId)
                .header("Authorization", "Bearer " + token)
                .retrieve().toBodilessEntity();
    }

    private DynoResultDTO runDyno(String token) {
        DynoResultDTO result = restClient().post().uri("/api/v1/dinamometro/executar")
                .header("Authorization", "Bearer " + token)
                .retrieve().body(DynoResultDTO.class);
        assertNotNull(result);
        assertNotNull(result.peakPower());
        assertNotNull(result.peakTorque());
        assertNotNull(result.finalStatus());
        return result;
    }

    private ChallengeResponseDTO selectChallenge(String token, Long challengeId) {
        ResponseEntity<ChallengeResponseDTO> response = restClient().post()
                .uri("/api/v1/challenges/" + challengeId + "/select")
                .header("Authorization", "Bearer " + token)
                .retrieve().toEntity(ChallengeResponseDTO.class);
        assertNotNull(response.getBody());
        return response.getBody();
    }

    private ChallengeResponseDTO getActiveChallenge(String token) {
        ResponseEntity<ChallengeResponseDTO> response = getActiveChallengeResponse(token);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }

    private ResponseEntity<ChallengeResponseDTO> getActiveChallengeResponse(String token) {
        return restClient().get().uri("/api/v1/challenges/active")
                .header("Authorization", "Bearer " + token)
                .retrieve().toEntity(ChallengeResponseDTO.class);
    }

    private PageResponseDTO<ChallengeResponseDTO> getAvailable(String token) {
        PageResponseDTO<ChallengeResponseDTO> page = restClient().get().uri("/api/v1/challenges/available")
                .header("Authorization", "Bearer " + token)
                .retrieve().body(new ParameterizedTypeReference<PageResponseDTO<ChallengeResponseDTO>>() {
                });
        assertNotNull(page);
        return page;
    }

    private ChallengePerformanceDTO getPerformance(String token, Long challengeId) {
        ChallengePerformanceDTO performance = restClient().get()
                .uri("/api/v1/challenges/" + challengeId + "/performance")
                .header("Authorization", "Bearer " + token)
                .retrieve().body(ChallengePerformanceDTO.class);
        assertNotNull(performance);
        return performance;
    }

    private ResponseEntity<PageResponseDTO<ChallengeResponseDTO>> getChallengesByClass(String token, Long classId) {
        ResponseEntity<PageResponseDTO<ChallengeResponseDTO>> response = restClient().get()
                .uri("/api/v1/challenges?classId=" + classId)
                .header("Authorization", "Bearer " + token)
                .retrieve().toEntity(new ParameterizedTypeReference<PageResponseDTO<ChallengeResponseDTO>>() {
                });
        assertNotNull(response.getBody());
        return response;
    }

    private HttpStatusCode statusOf(Supplier<ResponseEntity<?>> action) {
        try {
            return action.get().getStatusCode();
        } catch (RestClientResponseException exception) {
            return exception.getStatusCode();
        }
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

    private RestClient restClient() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
