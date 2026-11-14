package com.mapa.config;

import com.mapa.domain.enums.UserAuditAction;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@DependsOnDatabaseInitialization
public class AuditLogConstraintUpdater implements ApplicationRunner {
    private static final Pattern ACTION_LITERAL = Pattern.compile("'([A-Z_]+)'");
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        String databaseProduct = jdbcTemplate.execute((ConnectionCallback<String>)
                connection -> connection.getMetaData().getDatabaseProductName());
        if (!"PostgreSQL".equals(databaseProduct)) {
            return;
        }

        // Prevent concurrent application starts from replacing the same constraint.
        jdbcTemplate.execute("LOCK TABLE user_audit_logs IN SHARE UPDATE EXCLUSIVE MODE");
        String constraint = jdbcTemplate.query("""
                SELECT pg_get_constraintdef(oid)
                FROM pg_constraint
                WHERE conrelid = 'user_audit_logs'::regclass
                    AND conname = 'user_audit_logs_action_check' AND contype = 'c'
                """, result -> result.next() ? result.getString(1) : "");
        var allowedActions = ACTION_LITERAL.matcher(constraint).results()
                .map(match -> match.group(1))
                .collect(Collectors.toSet());
        var currentActions = Arrays.stream(UserAuditAction.values())
                .map(UserAuditAction::name)
                .collect(Collectors.toSet());
        if (allowedActions.equals(currentActions)) {
            return;
        }

        String actionList = currentActions.stream().sorted()
                .map(action -> "'" + action + "'")
                .collect(Collectors.joining(", "));
        jdbcTemplate.execute("""
                ALTER TABLE user_audit_logs
                    DROP CONSTRAINT IF EXISTS user_audit_logs_action_check,
                    ADD CONSTRAINT user_audit_logs_action_check CHECK (action IN (%s))
                """.formatted(actionList));
    }
}
