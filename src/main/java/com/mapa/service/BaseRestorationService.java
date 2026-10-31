package com.mapa.service;

import com.mapa.domain.enums.UserAuditAction;
import com.mapa.exception.BaseRestorationException;
import com.mapa.exception.InvalidCredentialsException;
import com.mapa.repository.ChallengeRepository;
import com.mapa.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class BaseRestorationService {
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final ChallengeRepository challengeRepository;
    private final UserAuditLogService auditLogService;

    @Transactional
    public void restoreMotors() {
        requireAdmin();
        if (challengeRepository.count() > 0) {
            throw new BaseRestorationException(
                    "Existem desafios vinculados aos motores. Remova esses desafios antes de restaurar a base.");
        }
        jdbcTemplate.update("DELETE FROM ecu_map_tunings");
        jdbcTemplate.update("UPDATE users SET active_motor_id = NULL");
        jdbcTemplate.update("DELETE FROM motors");
        populate("sql/restore-motors.sql");
        auditLogService.recordRequiredByCurrentUser(UserAuditAction.MOTOR_BASE_RESTORED,
                "Catálogo de motores substituído pelo SQL padrão; ajustes e seleções de motor removidos.");
    }

    @Transactional
    public void restoreMaps() {
        requireAdmin();
        jdbcTemplate.update("DELETE FROM ecu_map_tunings");
        jdbcTemplate.update("DELETE FROM ecu_map_defaults");
        populate("sql/restore-maps.sql");
        auditLogService.recordRequiredByCurrentUser(UserAuditAction.ECU_MAP_BASE_RESTORED,
                "Mapas de fábrica substituídos pelo SQL padrão; todos os ajustes personalizados removidos.");
    }

    private void populate(String resourcePath) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource(resourcePath));
        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        populator.execute(dataSource);
    }

    private void requireAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new InvalidCredentialsException();
        }
        if (!principal.isActive() || !"ADMIN".equals(principal.getRole())) {
            throw new AccessDeniedException("Apenas administradores ativos podem restaurar as bases.");
        }
    }
}
