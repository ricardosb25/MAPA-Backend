package com.mapa.service;

import com.mapa.domain.User;
import com.mapa.domain.enums.Role;
import com.mapa.dto.user.UserUpdateRequestDTO;
import com.mapa.exception.RoleNotAllowedException;
import com.mapa.repository.UserRepository;
import com.mapa.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:admin-concurrency;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;LOCK_TIMEOUT=10000")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdministratorConcurrencyIntegrationTest {
    @Autowired private UserRepository userRepository;
    @Autowired private UserService userService;

    @Test
    void shouldKeepAnAdministratorDuringConcurrentDemotions() throws Exception {
        User firstAdmin = userRepository.findAll().stream()
                .filter(account -> account.isActive() && account.getRole() == Role.ADMIN)
                .findFirst().orElseThrow();
        User secondAdmin = userRepository.saveAndFlush(User.builder().fullName("Administrador de teste")
                .email("admin-" + UUID.randomUUID() + "@mapa.test").passwordHash("unused-test-hash")
                .role(Role.ADMIN).active(true).build());
        var startSignal = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var firstAttempt = executor.submit(() -> attemptDemotion(firstAdmin, startSignal));
            var secondAttempt = executor.submit(() -> attemptDemotion(secondAdmin, startSignal));
            startSignal.countDown();
            int successfulDemotions = (firstAttempt.get(20, TimeUnit.SECONDS) ? 1 : 0)
                    + (secondAttempt.get(20, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successfulDemotions);
            assertEquals(1, userRepository.findAll().stream()
                    .filter(account -> account.isActive() && account.getRole() == Role.ADMIN).count());
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean attemptDemotion(User account, CountDownLatch startSignal) throws InterruptedException {
        assertTrue(startSignal.await(5, TimeUnit.SECONDS));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new UserPrincipal(account), null, List.of()));
        try {
            userService.update(account.getId(),
                    new UserUpdateRequestDTO(account.getFullName(), account.getEmail(), Role.TEACHER, null));
            return true;
        } catch (RoleNotAllowedException exception) {
            return false;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
