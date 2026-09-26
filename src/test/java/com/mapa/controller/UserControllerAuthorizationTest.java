package com.mapa.controller;

import com.mapa.domain.enums.Role;
import com.mapa.dto.auth.AuthResponseDTO;
import com.mapa.dto.auth.LoginRequestDTO;
import com.mapa.dto.auth.RegisterRequestDTO;
import com.mapa.dto.auth.UserResponseDTO;
import com.mapa.dto.user.UserCreateRequestDTO;
import com.mapa.dto.user.UserUpdateRequestDTO;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserControllerAuthorizationTest {

    private static final String ADMIN_EMAIL = "admin@mapa.test";
    private static final String ADMIN_PASSWORD = "AdminTest12345!";
    private static final String STANDARD_PASSWORD = "senha123456";

    @LocalServerPort
    private int port;

    @Test
    void shouldRejectAllUserEndpointsWithoutToken() {
        assertEquals(HttpStatus.UNAUTHORIZED, get(null, "/api/v1/users"));
        assertEquals(HttpStatus.UNAUTHORIZED, get(null, "/api/v1/users/1"));
        assertEquals(HttpStatus.UNAUTHORIZED, get(null, "/api/v1/users/audit-logs"));
        assertEquals(HttpStatus.UNAUTHORIZED, post(null, "/api/v1/users", createPayload("Sem Token")));
        assertEquals(HttpStatus.UNAUTHORIZED, put(null, "/api/v1/users/1", updatePayload("Sem Token", "sem@email.com")));
        assertEquals(HttpStatus.UNAUTHORIZED, delete(null, "/api/v1/users/1"));
    }

    @Test
    void shouldRestrictAdminOnlyEndpointsForCommonUsers() {
        UserResponseDTO student = registerStudent();
        String studentToken = login(student.email());

        assertEquals(HttpStatus.FORBIDDEN, get(studentToken, "/api/v1/users"));
        assertEquals(HttpStatus.FORBIDDEN, get(studentToken, "/api/v1/users/audit-logs"));
        assertEquals(HttpStatus.FORBIDDEN, post(studentToken, "/api/v1/users", createPayload("Criado Por Aluno")));
    }

    @Test
    void shouldAllowCommonUserToManageOnlyOwnAccount() {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        Long adminId = currentUser(adminToken).id();
        UserResponseDTO student = registerStudent();
        String studentToken = login(student.email());

        assertEquals(HttpStatus.OK, get(studentToken, "/api/v1/users/" + student.id()));
        assertEquals(HttpStatus.FORBIDDEN, get(studentToken, "/api/v1/users/" + adminId));
        assertEquals(HttpStatus.FORBIDDEN,
                put(studentToken, "/api/v1/users/" + adminId, updatePayload("Admin Editado", ADMIN_EMAIL)));
        assertEquals(HttpStatus.FORBIDDEN, delete(studentToken, "/api/v1/users/" + adminId));

        assertEquals(HttpStatus.OK,
                put(studentToken, "/api/v1/users/" + student.id(), updatePayload("Aluno Renomeado", student.email())));

        UserUpdateRequestDTO roleChangeAttempt = new UserUpdateRequestDTO(
                student.fullName(), student.email(), Role.ADMIN, null);
        assertEquals(HttpStatus.FORBIDDEN, put(studentToken, "/api/v1/users/" + student.id(), roleChangeAttempt));
    }

    @Test
    void shouldAllowAdminFullCrudOverAnyUserAndReadAuditLogs() {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        UserCreateRequestDTO payload = createPayload("Aluno Sob Admin");

        ResponseEntity<UserResponseDTO> createResponse =
                executePost(adminToken, "/api/v1/users", payload, UserResponseDTO.class);
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        Long createdUserId = createResponse.getBody().id();

        assertEquals(HttpStatus.OK, get(adminToken, "/api/v1/users"));
        assertEquals(HttpStatus.OK, get(adminToken, "/api/v1/users/" + createdUserId));
        assertEquals(HttpStatus.OK,
                put(adminToken, "/api/v1/users/" + createdUserId, updatePayload("Aluno Editado", createResponse.getBody().email())));
        assertEquals(HttpStatus.OK, get(adminToken, "/api/v1/users/audit-logs"));

        assertEquals(HttpStatus.NO_CONTENT, delete(adminToken, "/api/v1/users/" + createdUserId));

        String anonymizedLogs = getBody(adminToken,
                "/api/v1/users/audit-logs?action=USER_ANONYMIZED&targetUserId=" + createdUserId);
        assertTrue(anonymizedLogs.contains("USER_ANONYMIZED"),
                "A auditoria deve registrar a anonimização do usuário");
        assertTrue(anonymizedLogs.contains(createResponse.getBody().email()),
                "O e-mail original deve ser preservado na auditoria");
    }

    @Test
    void shouldBlockAnonymizationOfLastActiveAdmin() {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        Long adminId = currentUser(adminToken).id();

        assertEquals(HttpStatus.FORBIDDEN, delete(adminToken, "/api/v1/users/" + adminId));
    }

    @Test
    void shouldInvalidateTokenAfterSelfAnonymization() {
        UserResponseDTO student = registerStudent();
        String studentToken = login(student.email());

        assertEquals(HttpStatus.NO_CONTENT, delete(studentToken, "/api/v1/users/" + student.id()));
        assertEquals(HttpStatus.UNAUTHORIZED, get(studentToken, "/api/v1/auth/me"));
    }

    private RestClient restClient() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    private UserResponseDTO registerStudent() {
        ResponseEntity<UserResponseDTO> response = restClient().post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequestDTO("Aluno Teste", uniqueEmail(), STANDARD_PASSWORD, Role.STUDENT))
                .retrieve().toEntity(UserResponseDTO.class);
        assertNotNull(response.getBody());
        return response.getBody();
    }

    private String login(String email) {
        return login(email, STANDARD_PASSWORD);
    }

    private String login(String email, String password) {
        ResponseEntity<AuthResponseDTO> response = restClient().post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequestDTO(email, password))
                .retrieve().toEntity(AuthResponseDTO.class);
        assertNotNull(response.getBody());
        return response.getBody().token();
    }

    private UserResponseDTO currentUser(String bearerToken) {
        ResponseEntity<UserResponseDTO> response = restClient().get().uri("/api/v1/auth/me")
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve().toEntity(UserResponseDTO.class);
        assertNotNull(response.getBody());
        return response.getBody();
    }

    private HttpStatusCode get(String bearerToken, String uri) {
        try {
            return withBearer(restClient().get().uri(uri), bearerToken)
                    .retrieve().toBodilessEntity().getStatusCode();
        } catch (RestClientResponseException exception) {
            return exception.getStatusCode();
        }
    }

    private String getBody(String bearerToken, String uri) {
        try {
            return withBearer(restClient().get().uri(uri), bearerToken)
                    .retrieve().body(String.class);
        } catch (RestClientResponseException exception) {
            return exception.getResponseBodyAsString();
        }
    }

    private HttpStatusCode post(String bearerToken, String uri, Object body) {
        try {
            return executePost(bearerToken, uri, body, Void.class).getStatusCode();
        } catch (RestClientResponseException exception) {
            return exception.getStatusCode();
        }
    }

    private HttpStatusCode put(String bearerToken, String uri, Object body) {
        try {
            return withBearer(restClient().put().uri(uri).contentType(MediaType.APPLICATION_JSON).body(body), bearerToken)
                    .retrieve().toBodilessEntity().getStatusCode();
        } catch (RestClientResponseException exception) {
            return exception.getStatusCode();
        }
    }

    private HttpStatusCode delete(String bearerToken, String uri) {
        try {
            return withBearer(restClient().delete().uri(uri), bearerToken)
                    .retrieve().toBodilessEntity().getStatusCode();
        } catch (RestClientResponseException exception) {
            return exception.getStatusCode();
        }
    }

    private <T> ResponseEntity<T> executePost(String bearerToken, String uri, Object body, Class<T> responseType) {
        return withBearer(restClient().post().uri(uri).contentType(MediaType.APPLICATION_JSON).body(body), bearerToken)
                .retrieve().toEntity(responseType);
    }

    private RestClient.RequestHeadersSpec<?> withBearer(RestClient.RequestHeadersSpec<?> request, String bearerToken) {
        if (bearerToken == null) {
            return request;
        }
        return request.header("Authorization", "Bearer " + bearerToken);
    }

    private UserCreateRequestDTO createPayload(String fullName) {
        return new UserCreateRequestDTO(fullName, uniqueEmail(), STANDARD_PASSWORD, Role.STUDENT);
    }

    private UserUpdateRequestDTO updatePayload(String fullName, String email) {
        return new UserUpdateRequestDTO(fullName, email, null, null);
    }

    private String uniqueEmail() {
        return "aluno-" + UUID.randomUUID().toString().substring(0, 8) + "@mapa.test";
    }
}
