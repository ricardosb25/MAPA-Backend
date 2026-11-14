package com.mapa.controller;

import com.mapa.domain.enums.Role;
import com.mapa.domain.enums.Shift;
import com.mapa.dto.PageResponseDTO;
import com.mapa.dto.auth.AuthResponseDTO;
import com.mapa.dto.auth.LoginRequestDTO;
import com.mapa.dto.auth.RegisterRequestDTO;
import com.mapa.dto.schoolClass.ClassStudentResponseDTO;
import com.mapa.dto.schoolClass.ClassDetailResponseDTO;
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

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SchoolClassControllerAuthorizationTest {

    private static final String PASSWORD = "password123";

    @LocalServerPort
    private int port;

    private RestClient restClient() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void shouldRejectClassEndpointsWithoutToken() {
        assertEquals(HttpStatus.UNAUTHORIZED, statusOf(() -> getClasses(null, null, 0, 10)));
        assertEquals(HttpStatus.UNAUTHORIZED, statusOf(() -> createClass(null, "Turma Sem Token", Shift.EVENING)));
        assertEquals(HttpStatus.UNAUTHORIZED, statusOf(() -> getMyClasses(null, null, 0, 10)));
        assertEquals(HttpStatus.UNAUTHORIZED, statusOf(() -> join(null, "AUTO-00")));
    }

    @Test
    void shouldAllowTeacherToManageOnlyOwnClasses() {
        String ownerToken = registerAndGetToken("Professora Helena", Role.TEACHER);
        String otherTeacherToken = registerAndGetToken("Professor Marcos", Role.TEACHER);

        ClassResponseDTO createdClass = createClass(ownerToken, "Eletronica Embarcada " + uniqueSuffix(), Shift.AFTERNOON).getBody();
        assertNotNull(createdClass.code());
        assertEquals(0L, createdClass.studentCount());

        assertEquals(HttpStatus.OK, statusOf(() -> getClassById(ownerToken, createdClass.id())));
        assertEquals(HttpStatus.OK, statusOf(() -> updateClass(ownerToken, createdClass.id(), "Eletronica Embarcada Editada")));

        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> getClassById(otherTeacherToken, createdClass.id())));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> updateClass(otherTeacherToken, createdClass.id(), "Nome Indevido")));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> regenerateCode(otherTeacherToken, createdClass.id())));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> deleteClass(otherTeacherToken, createdClass.id())));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> removeStudent(otherTeacherToken, createdClass.id(), 1L)));

        PageResponseDTO<ClassResponseDTO> ownerPage = getClasses(ownerToken, null, 0, 100).getBody();
        PageResponseDTO<ClassResponseDTO> otherPage = getClasses(otherTeacherToken, null, 0, 100).getBody();
        assertNotNull(ownerPage);
        assertNotNull(otherPage);
        assertTrue(ownerPage.content().stream().anyMatch(schoolClass -> schoolClass.id().equals(createdClass.id())));
        assertTrue(otherPage.content().stream().noneMatch(schoolClass -> schoolClass.id().equals(createdClass.id())));

        assertEquals(HttpStatus.NO_CONTENT, statusOf(() -> deleteClass(ownerToken, createdClass.id())));
        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> getClassById(ownerToken, createdClass.id())));
    }

    @Test
    void shouldRestrictEndpointsByRole() {
        String teacherToken = registerAndGetToken("Professora Role", Role.TEACHER);
        String studentToken = registerAndGetToken("Aluno Role", Role.STUDENT);

        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> getClasses(studentToken, null, 0, 10)));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> createClass(studentToken, "Turma Do Aluno", Shift.MORNING)));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> getMyClasses(teacherToken, null, 0, 10)));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> join(teacherToken, "AUTO-00")));

        assertEquals(HttpStatus.OK, statusOf(() -> getMyClasses(studentToken, null, 0, 10)));
    }
    @Test
    void shouldLetStudentJoinFilterAndLeaveClassesByCode() {
        String teacherToken = registerAndGetToken("Professora Fluxo", Role.TEACHER);
        String studentToken = registerAndGetToken("Aluno Fluxo", Role.STUDENT);
        String suffix = uniqueSuffix();

        ClassResponseDTO nightClass = createClass(teacherToken, "Noite Algoritmos " + suffix, Shift.EVENING).getBody();
        ClassResponseDTO morningClass = createClass(teacherToken, "Manha Algoritmos " + suffix, Shift.MORNING).getBody();

        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> join(studentToken, "XXXX-99")));
        assertEquals(HttpStatus.OK, statusOf(() -> join(studentToken, nightClass.code())));
        assertEquals(HttpStatus.CONFLICT, statusOf(() -> join(studentToken, nightClass.code())));

        PageResponseDTO<ClassResponseDTO> enrolledPage =
                getMyClasses(studentToken, "Algoritmos " + suffix, null, 0, 100).getBody();
        assertNotNull(enrolledPage);
        assertEquals(1, enrolledPage.totalElements());
        assertEquals(nightClass.id(), enrolledPage.content().get(0).id());

        PageResponseDTO<ClassResponseDTO> morningPage =
                getMyClasses(studentToken, null, Shift.MORNING, 0, 100).getBody();
        assertNotNull(morningPage);
        assertTrue(morningPage.content().stream().noneMatch(schoolClass -> schoolClass.id().equals(nightClass.id())));

        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> getClassById(studentToken, nightClass.id())));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> removeStudent(studentToken, nightClass.id(), 1L)));
        assertEquals(HttpStatus.FORBIDDEN, statusOf(() -> regenerateCode(studentToken, nightClass.id())));

        List<ClassStudentResponseDTO> enrolledStudents = getStudents(teacherToken, nightClass.id()).getBody();
        assertNotNull(enrolledStudents);
        assertEquals(1, enrolledStudents.size());
        assertEquals(HttpStatus.NO_CONTENT,
                statusOf(() -> removeStudent(teacherToken, nightClass.id(), enrolledStudents.get(0).studentId())));

        assertEquals(HttpStatus.OK, statusOf(() -> join(studentToken, nightClass.code())));
        assertEquals(HttpStatus.NO_CONTENT, statusOf(() -> leave(studentToken, nightClass.id())));
        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> leave(studentToken, nightClass.id())));

        PageResponseDTO<ClassResponseDTO> afterLeavePage = getMyClasses(studentToken, null, 0, 100).getBody();
        assertNotNull(afterLeavePage);
        assertTrue(afterLeavePage.content().stream().noneMatch(schoolClass -> schoolClass.id().equals(nightClass.id())));
        assertTrue(afterLeavePage.content().stream().noneMatch(schoolClass -> schoolClass.id().equals(morningClass.id())));
    }

    @Test
    void shouldRegenerateClassCodeInvalidatingTheOldOne() {
        String teacherToken = registerAndGetToken("Professora Codigo", Role.TEACHER);
        String studentToken = registerAndGetToken("Aluno Codigo", Role.STUDENT);

        ClassResponseDTO schoolClass = createClass(teacherToken, "Dinamometro " + uniqueSuffix(), Shift.MORNING).getBody();
        String originalCode = schoolClass.code();

        ClassResponseDTO regeneratedClass = regenerateCode(teacherToken, schoolClass.id()).getBody();
        assertNotNull(regeneratedClass);
        assertNotEquals(originalCode, regeneratedClass.code());

        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> join(studentToken, originalCode)));
        assertEquals(HttpStatus.OK, statusOf(() -> join(studentToken, regeneratedClass.code())));
    }

    @Test
    void shouldFilterAndPaginateTeacherClassList() {
        String teacherToken = registerAndGetToken("Professora Filtro", Role.TEACHER);
        String suffix = uniqueSuffix();

        ClassResponseDTO morningClass = createClass(teacherToken, "Calibracao Manha " + suffix, Shift.MORNING).getBody();
        ClassResponseDTO nightClass = createClass(teacherToken, "Calibracao Noite " + suffix, Shift.EVENING).getBody();

        PageResponseDTO<ClassResponseDTO> searchPage =
                getClasses(teacherToken, "Calibracao", null, 0, 100).getBody();
        assertNotNull(searchPage);
        assertTrue(searchPage.totalElements() >= 2);
        assertTrue(searchPage.content().stream().anyMatch(schoolClass -> schoolClass.id().equals(morningClass.id())));

        PageResponseDTO<ClassResponseDTO> shiftPage =
                getClasses(teacherToken, "Calibracao", Shift.EVENING, 0, 100).getBody();
        assertNotNull(shiftPage);
        assertEquals(1, shiftPage.totalElements());
        assertEquals(nightClass.id(), shiftPage.content().get(0).id());

        PageResponseDTO<ClassResponseDTO> firstPage =
                getClasses(teacherToken, "Calibracao", null, 0, 1).getBody();
        assertNotNull(firstPage);
        assertEquals(1, firstPage.content().size());
        assertEquals(2, firstPage.totalElements());
        assertEquals(2, firstPage.totalPages());
        assertTrue(firstPage.hasNext());
        assertFalse(firstPage.hasPrevious());
    }

    private ResponseEntity<PageResponseDTO<ClassResponseDTO>> getClasses(
            String token, String search, int page, int size) {
        return getClasses(token, search, null, page, size);
    }

    private ResponseEntity<PageResponseDTO<ClassResponseDTO>> getClasses(
            String token, String search, Shift shift, int page, int size) {
        return executeGet(token, buildUri("/api/v1/classes", search, shift, page, size),
                new ParameterizedTypeReference<PageResponseDTO<ClassResponseDTO>>() { });
    }

    private ResponseEntity<PageResponseDTO<ClassResponseDTO>> getMyClasses(
            String token, String search, int page, int size) {
        return getMyClasses(token, search, null, page, size);
    }

    private ResponseEntity<PageResponseDTO<ClassResponseDTO>> getMyClasses(
            String token, String search, Shift shift, int page, int size) {
        return executeGet(token, buildUri("/api/v1/classes/mine", search, shift, page, size),
                new ParameterizedTypeReference<PageResponseDTO<ClassResponseDTO>>() { });
    }

    private ResponseEntity<ClassDetailResponseDTO> getClassById(String token, Long classId) {
        return executeGet(token, "/api/v1/classes/" + classId,
                new ParameterizedTypeReference<ClassDetailResponseDTO>() { });
    }

    private ResponseEntity<List<ClassStudentResponseDTO>> getStudents(String token, Long classId) {
        return executeGet(token, "/api/v1/classes/" + classId + "/students",
                new ParameterizedTypeReference<List<ClassStudentResponseDTO>>() { });
    }

    private <T> ResponseEntity<T> executeGet(String token, String uri, ParameterizedTypeReference<T> responseType) {
        RestClient.RequestHeadersSpec<?> request = restClient().get().uri(uri);
        return withBearer(request, token).retrieve().toEntity(responseType);
    }

    private String buildUri(String baseUri, String search, Shift shift, int page, int size) {
        StringBuilder uri = new StringBuilder(baseUri)
                .append("?page=").append(page)
                .append("&size=").append(size);
        if (search != null && !search.isBlank()) {
            uri.append("&search=").append(search.trim());
        }
        if (shift != null) {
            uri.append("&shift=").append(shift.name());
        }
        return uri.toString();
    }

    private ResponseEntity<ClassResponseDTO> createClass(String token, String name, Shift shift) {
        RestClient.RequestHeadersSpec<?> request = restClient().post().uri("/api/v1/classes")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ClassRequestDTO(name, shift));
        return withBearer(request, token).retrieve().toEntity(ClassResponseDTO.class);
    }

    private ResponseEntity<ClassResponseDTO> updateClass(String token, Long classId, String name) {
        RestClient.RequestHeadersSpec<?> request = restClient().put().uri("/api/v1/classes/" + classId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ClassRequestDTO(name, Shift.AFTERNOON));
        return withBearer(request, token).retrieve().toEntity(ClassResponseDTO.class);
    }

    private ResponseEntity<ClassResponseDTO> regenerateCode(String token, Long classId) {
        RestClient.RequestHeadersSpec<?> request = restClient().post()
                .uri("/api/v1/classes/" + classId + "/new-code");
        return withBearer(request, token).retrieve().toEntity(ClassResponseDTO.class);
    }

    private ResponseEntity<Void> deleteClass(String token, Long classId) {
        RestClient.RequestHeadersSpec<?> request = restClient().delete().uri("/api/v1/classes/" + classId);
        return withBearer(request, token).retrieve().toBodilessEntity();
    }

    private ResponseEntity<Void> removeStudent(String token, Long classId, Long studentId) {
        RestClient.RequestHeadersSpec<?> request =
                restClient().delete().uri("/api/v1/classes/" + classId + "/students/" + studentId);
        return withBearer(request, token).retrieve().toBodilessEntity();
    }

    private ResponseEntity<ClassResponseDTO> join(String token, String code) {
        RestClient.RequestHeadersSpec<?> request = restClient().post().uri("/api/v1/classes/join")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ClassJoinRequestDTO(code));
        return withBearer(request, token).retrieve().toEntity(ClassResponseDTO.class);
    }

    private ResponseEntity<Void> leave(String token, Long classId) {
        RestClient.RequestHeadersSpec<?> request = restClient().delete().uri("/api/v1/classes/" + classId + "/leave");
        return withBearer(request, token).retrieve().toBodilessEntity();
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