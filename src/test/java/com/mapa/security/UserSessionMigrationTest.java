package com.mapa.security;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserSessionMigrationIntegrationTest {
    @Test
    void shouldInitializeExistingAccountsWithoutResettingSessionVersions() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:migration-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbcTemplate = new JdbcTemplate(dataSource);
        try {
            jdbcTemplate.execute("CREATE TABLE users (id BIGINT PRIMARY KEY)");
            jdbcTemplate.update("INSERT INTO users VALUES (1)");
            var migration = new ResourceDatabasePopulator(
                    new ClassPathResource("sql/migrations/2026-11-03-p1-session-version.sql"));
            migration.execute(dataSource);
            assertEquals(0L, jdbcTemplate.queryForObject("SELECT credentials_version FROM users WHERE id = 1", Long.class));
            assertEquals(0L, jdbcTemplate.queryForObject("SELECT row_version FROM users WHERE id = 1", Long.class));
            jdbcTemplate.update("UPDATE users SET credentials_version = 3, row_version = 7 WHERE id = 1");
            migration.execute(dataSource);
            assertEquals(3L, jdbcTemplate.queryForObject("SELECT credentials_version FROM users WHERE id = 1", Long.class));
            assertEquals(7L, jdbcTemplate.queryForObject("SELECT row_version FROM users WHERE id = 1", Long.class));
        } finally {
            jdbcTemplate.execute("SHUTDOWN");
        }
    }
}
