package com.mapa.controller;

import com.mapa.domain.SchoolClass;
import com.mapa.domain.User;
import com.mapa.domain.enums.Role;
import com.mapa.domain.enums.Shift;
import com.mapa.repository.SchoolClassRepository;
import com.mapa.repository.UserRepository;
import com.mapa.security.JwtService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherDashboardAuthorizationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private SchoolClassRepository classRepository;
    @Autowired private JwtService jwtService;
    private final List<User> accounts = new ArrayList<>();
    private final List<SchoolClass> schoolClasses = new ArrayList<>();

    @AfterEach
    void removeTestData() {
        classRepository.deleteAll(schoolClasses);
        userRepository.deleteAll(accounts);
    }

    @Test
    void shouldReturnDashboardForTeacherWithRealJwt() throws Exception {
        User teacher = createAccount(Role.TEACHER);
        SchoolClass schoolClass = classRepository.save(SchoolClass.builder().name("Turma de teste")
                .code(UUID.randomUUID().toString().substring(0, 8)).shift(Shift.EVENING).teacher(teacher).build());
        schoolClasses.add(schoolClass);
        String response = mockMvc.perform(authenticatedRequest(teacher, "/api/v1/teacher/dashboard"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(response.contains("Turma de teste"));
        assertTrue(response.contains("\"classId\":" + schoolClass.getId()));
    }

    @Test
    void shouldRejectMissingTokenAndOtherProfiles() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/dashboard")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/error")).andExpect(status().isUnauthorized());
        for (Role role : List.of(Role.STUDENT, Role.ADMIN)) {
            User account = createAccount(role);
            mockMvc.perform(authenticatedRequest(account, "/api/v1/teacher/dashboard"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void shouldPreserveOriginalErrorStatusOnErrorDispatch() throws Exception {
        for (HttpStatus errorStatus : List.of(HttpStatus.NOT_FOUND, HttpStatus.INTERNAL_SERVER_ERROR)) {
            mockMvc.perform(get("/error").with(servletRequest -> {
                        servletRequest.setDispatcherType(DispatcherType.ERROR);
                        return servletRequest;
                    })
                    .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, errorStatus.value())
                    .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/v1/teacher/dashboard"))
                    .andExpect(status().is(errorStatus.value()));
        }
    }

    private User createAccount(Role role) {
        User account = userRepository.save(User.builder().fullName("Conta de teste")
                .email("dashboard-" + UUID.randomUUID() + "@mapa.test")
                .passwordHash("unused-test-hash").role(role).active(true).build());
        accounts.add(account);
        return account;
    }

    private MockHttpServletRequestBuilder authenticatedRequest(User account, String path) {
        return get(path).header("Authorization", "Bearer " + jwtService.generateToken(account.getEmail(), account.getRole().name()));
    }
}
