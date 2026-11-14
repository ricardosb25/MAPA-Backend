package com.mapa.controller;

import com.mapa.domain.enums.Role;
import com.mapa.dto.EcuMapCellUpdateRequestDTO;
import com.mapa.dto.EcuMapResponseDTO;
import com.mapa.dto.MotorRequestDTO;
import com.mapa.dto.MotorResponseDTO;
import com.mapa.dto.MotorSelectionRequestDTO;
import com.mapa.dto.auth.AuthResponseDTO;
import com.mapa.dto.auth.LoginRequestDTO;
import com.mapa.dto.auth.RegisterRequestDTO;
import com.mapa.dto.auth.UserResponseDTO;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EcuMapControllerAuthorizationTest {

    private static final String ADMIN_EMAIL = "admin@mapa.test";
    private static final String ADMIN_PASSWORD = "AdminTest12345!";

    @LocalServerPort
    private int port;

    @Test
    void shouldRejectMapAndSelectionEndpointsWithoutToken() {
        assertEquals(HttpStatus.UNAUTHORIZED, getStatus(null, "/api/v1/motores/1/mapas", HttpMethod.GET));
        assertEquals(HttpStatus.UNAUTHORIZED, getStatus(null, "/api/v1/motores/selecionado", HttpMethod.GET));
        assertEquals(HttpStatus.UNAUTHORIZED, getStatus(null, "/api/v1/motores/selecionado", HttpMethod.DELETE));
        assertEquals(HttpStatus.UNAUTHORIZED, getStatus(null, "/api/v1/motores/1/mapas/IGNITION/celulas", HttpMethod.PUT));
        assertEquals(HttpStatus.UNAUTHORIZED, getStatus(null, "/api/v1/motores/1/mapas/IGNITION/restaurar", HttpMethod.POST));
    }

    @Test
    void shouldAllowEveryAuthenticatedProfileToReadEcuMaps() {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        Long motorId = adminCreatesMotor(adminToken);

        assertEquals(HttpStatus.OK, getStatus(registerUserAndGetToken(Role.STUDENT),
                "/api/v1/motores/" + motorId + "/mapas", HttpMethod.GET));
        assertEquals(HttpStatus.OK, getStatus(registerUserAndGetToken(Role.TEACHER),
                "/api/v1/motores/" + motorId + "/mapas", HttpMethod.GET));
        assertEquals(HttpStatus.OK, getStatus(adminToken,
                "/api/v1/motores/" + motorId + "/mapas", HttpMethod.GET));
    }

    private RestClient restClient() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    private enum HttpMethod { GET, PUT, DELETE, POST }

    private HttpStatusCode getStatus(String bearerToken, String uri, HttpMethod method) {
        try {
            RestClient.RequestHeadersSpec<?> request = switch (method) {
                case GET -> restClient().get().uri(uri);
                case PUT -> restClient().put().uri(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new EcuMapCellUpdateRequestDTO(0, 0, 12));
                case POST -> restClient().post().uri(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new EcuMapCellUpdateRequestDTO(0, 0, 12));
                case DELETE -> restClient().delete().uri(uri);
            };

            return withBearer(request, bearerToken)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode();
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

    @Test
    void shouldPersistEditedCellPerUserAndRestoreFactoryMap() {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        Long motorId = adminCreatesMotor(adminToken);
        String studentToken = registerUserAndGetToken(Role.STUDENT);
        String otherStudentToken = registerUserAndGetToken(Role.STUDENT);
        String mapsUri = "/api/v1/motores/" + motorId + "/mapas";

        EcuMapResponseDTO editedMap = executePutCell(studentToken, mapsUri + "/IGNITION/celulas",
                new EcuMapCellUpdateRequestDTO(0, 3, 18));
        assertFalse(editedMap.factoryDefault());
        assertEquals(18, editedMap.values().get(0).get(3));

        List<EcuMapResponseDTO> persistedMaps = executeGetMaps(studentToken, mapsUri);
        EcuMapResponseDTO persistedIgnition = findMap(persistedMaps, "IGNITION");
        assertEquals(18, persistedIgnition.values().get(0).get(3));
        assertFalse(persistedIgnition.factoryDefault());

        EcuMapResponseDTO untouchedMap = findMap(executeGetMaps(otherStudentToken, mapsUri), "IGNITION");
        assertTrue(untouchedMap.factoryDefault());
        assertFalse(untouchedMap.values().get(0).get(3) == 18);

        EcuMapResponseDTO restoredMap = executePostRestore(studentToken, mapsUri + "/IGNITION/restaurar");
        assertTrue(restoredMap.factoryDefault());
        assertTrue(findMap(executeGetMaps(studentToken, mapsUri), "IGNITION").factoryDefault());
    }

    @Test
    void shouldRejectCellValueOutsideRealisticEcuRange() {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        Long motorId = adminCreatesMotor(adminToken);
        String studentToken = registerUserAndGetToken(Role.STUDENT);
        String cellUri = "/api/v1/motores/" + motorId + "/mapas/IGNITION/celulas";

        RestClientResponseException exception = null;
        try {
            executePutCell(studentToken, cellUri, new EcuMapCellUpdateRequestDTO(0, 0, 999));
        } catch (RestClientResponseException caughtException) {
            exception = caughtException;
        }

        assertNotNull(exception);
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getResponseBodyAsString().contains("fora do intervalo"));
    }

    @Test
    void shouldPersistActiveEnginePerUser() {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        Long motorId = adminCreatesMotor(adminToken);
        String studentToken = registerUserAndGetToken(Role.STUDENT);
        String selectionUri = "/api/v1/motores/selecionado";

        assertNull(currentUser(studentToken).activeMotorId());

        ResponseEntity<MotorResponseDTO> selectResponse = restClient().put().uri(selectionUri)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + studentToken)
                .body(new MotorSelectionRequestDTO(motorId))
                .retrieve().toEntity(MotorResponseDTO.class);
        assertEquals(HttpStatus.OK, selectResponse.getStatusCode());
        assertNotNull(selectResponse.getBody());
        assertEquals(motorId, selectResponse.getBody().id());
        assertEquals(motorId, currentUser(studentToken).activeMotorId());

        ResponseEntity<MotorResponseDTO> findResponse = restClient().get().uri(selectionUri)
                .header("Authorization", "Bearer " + studentToken)
                .retrieve().toEntity(MotorResponseDTO.class);
        assertEquals(HttpStatus.OK, findResponse.getStatusCode());
        assertNotNull(findResponse.getBody());
        assertEquals(motorId, findResponse.getBody().id());

        HttpStatusCode clearStatus = restClient().delete().uri(selectionUri)
                .header("Authorization", "Bearer " + studentToken)
                .retrieve().toBodilessEntity().getStatusCode();
        assertEquals(HttpStatus.NO_CONTENT, clearStatus);
        assertNull(currentUser(studentToken).activeMotorId());

        ResponseEntity<MotorResponseDTO> emptyResponse = restClient().get().uri(selectionUri)
                .header("Authorization", "Bearer " + studentToken)
                .retrieve().toEntity(MotorResponseDTO.class);
        assertEquals(HttpStatus.NO_CONTENT, emptyResponse.getStatusCode());
    }

    private UserResponseDTO currentUser(String bearerToken) {
        ResponseEntity<UserResponseDTO> response = restClient().get().uri("/api/v1/auth/me")
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve().toEntity(UserResponseDTO.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }

    private EcuMapResponseDTO findMap(List<EcuMapResponseDTO> maps, String mapType) {
        return maps.stream()
                .filter(map -> map.mapType().equals(mapType))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Mapa " + mapType + " não encontrado"));
    }

    private List<EcuMapResponseDTO> executeGetMaps(String bearerToken, String mapsUri) {
        List<EcuMapResponseDTO> maps = restClient().get().uri(mapsUri)
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve().body(new ParameterizedTypeReference<List<EcuMapResponseDTO>>() { });
        assertNotNull(maps);
        assertEquals(2, maps.size());
        return maps;
    }

    private EcuMapResponseDTO executePutCell(String bearerToken, String cellUri, EcuMapCellUpdateRequestDTO cell) {
        EcuMapResponseDTO updatedMap = restClient().put().uri(cellUri)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + bearerToken)
                .body(cell)
                .retrieve().body(EcuMapResponseDTO.class);
        assertNotNull(updatedMap);
        return updatedMap;
    }

    private EcuMapResponseDTO executePostRestore(String bearerToken, String restoreUri) {
        EcuMapResponseDTO restoredMap = restClient().post().uri(restoreUri)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve().body(EcuMapResponseDTO.class);
        assertNotNull(restoredMap);
        return restoredMap;
    }

    private String registerUserAndGetToken(Role role) {
        String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + "@email.com";
        ResponseEntity<?> registerResponse = restClient().post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequestDTO("Usuário Teste", email, "password123", role, true))
                .retrieve().toEntity(Object.class);
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());

        return login(email, "password123");
    }

    private String login(String email, String password) {
        AuthResponseDTO authResponse = restClient().post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequestDTO(email, password))
                .retrieve().body(AuthResponseDTO.class);

        assertNotNull(authResponse);
        return authResponse.token();
    }

    private Long adminCreatesMotor(String adminToken) {
        MotorRequestDTO payload = new MotorRequestDTO(
                "Volkswagen",
                "AP 1.8 8V " + UUID.randomUUID().toString().substring(0, 8),
                "Motor didático para testes de mapa",
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
}
