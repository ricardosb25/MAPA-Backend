package com.mapa.service;

import com.mapa.domain.enums.UserAuditAction;
import com.mapa.exception.BaseRestorationException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PresentationModeService {
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final UserAuditLogService auditLogService;

    public record Status(boolean enabled, String teacherName) {
    }

    @Transactional(readOnly = true)
    public Status getStatus() {
        return jdbcTemplate.query("""
                SELECT teacher.full_name
                FROM presentation_data data
                LEFT JOIN users teacher ON teacher.id = data.record_id
                WHERE data.record_type = 'TEACHER'
                """, result -> result.next()
                ? new Status(true, result.getString("full_name"))
                : new Status(false, null));
    }

    @Transactional
    public Status setEnabled(boolean enabled) {
        jdbcTemplate.execute("LOCK TABLE presentation_data IN SHARE ROW EXCLUSIVE MODE");
        Status current = getStatus();
        if (current.enabled() == enabled) {
            return current;
        }

        if (enabled) {
            Long teacherId = jdbcTemplate.query("""
                    SELECT id FROM users WHERE role = 'TEACHER' AND active ORDER BY id LIMIT 1 FOR SHARE
                    """, result -> result.next() ? result.getLong("id") : null);
            if (teacherId == null) {
                throw new BaseRestorationException("Cadastre um professor ativo para receber as turmas de demonstração.");
            }
            Boolean hasMotor = jdbcTemplate.queryForObject("SELECT EXISTS (SELECT 1 FROM motors)", Boolean.class);
            if (!Boolean.TRUE.equals(hasMotor)) {
                throw new BaseRestorationException("Cadastre ou restaure pelo menos um motor para ativar o modo apresentação.");
            }
            jdbcTemplate.update("INSERT INTO presentation_data (record_type, record_id) VALUES ('TEACHER', ?)", teacherId);
        }

        String resourcePath = enabled ? "sql/enable-presentation-mode.sql" : "sql/disable-presentation-mode.sql";
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource(resourcePath));
        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        populator.setSeparator(ScriptUtils.EOF_STATEMENT_SEPARATOR);
        populator.execute(dataSource);

        auditLogService.recordRequiredByCurrentUser(
                enabled ? UserAuditAction.PRESENTATION_MODE_ENABLED : UserAuditAction.PRESENTATION_MODE_DISABLED,
                enabled ? "Dados de demonstração inseridos." : "Dados de demonstração removidos.");
        return getStatus();
    }
}
