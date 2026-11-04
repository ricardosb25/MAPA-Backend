package com.mapa.security;

import com.mapa.config.properties.JwtProperties;
import com.mapa.domain.User;
import com.mapa.domain.enums.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtServiceWithSecret(String secret) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        properties.setExpirationMs(3600000L);
        return new JwtService(properties);
    }

    @Test
    void shouldGenerateAndValidateToken() {
        JwtService jwtService = jwtServiceWithSecret("segredo-de-teste-com-mais-de-32-caracteres-123");

        User user = User.builder().id(1L).email("aluno@email.com").role(Role.STUDENT).active(true).build();
        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token));
        assertEquals("aluno@email.com", jwtService.extractEmail(token));
        assertTrue(jwtService.isTokenValid(token, new UserPrincipal(user)));
    }

    @Test
    void shouldRejectInvalidToken() {
        JwtService jwtService = jwtServiceWithSecret("segredo-de-teste-com-mais-de-32-caracteres-123");

        assertFalse(jwtService.isTokenValid("token.invalido.aqui"));
    }

    @Test
    void shouldRejectLegacyTokensWithoutSessionClaims() {
        String secret = "segredo-de-teste-com-mais-de-32-caracteres-123";
        JwtService jwtService = jwtServiceWithSecret(secret);
        String legacyToken = Jwts.builder().subject("aluno@email.com")
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
        User user = User.builder().id(1L).email("aluno@email.com").role(Role.STUDENT).active(true).build();
        assertFalse(jwtService.isTokenValid(legacyToken));
        assertFalse(jwtService.isTokenValid(legacyToken, new UserPrincipal(user)));
    }

    @Test
    void shouldRejectTokensFromAnotherAccountOrCredentialVersion() {
        JwtService jwtService = jwtServiceWithSecret("segredo-de-teste-com-mais-de-32-caracteres-123");
        User user = User.builder().id(1L).email("aluno@email.com").role(Role.STUDENT).active(true).build();
        String token = jwtService.generateToken(user);
        user.invalidateSessions();
        assertFalse(jwtService.isTokenValid(token, new UserPrincipal(user)));
        user.setCredentialsVersion(0);
        user.setId(2L);
        assertFalse(jwtService.isTokenValid(token, new UserPrincipal(user)));
    }

    @Test
    void shouldRejectTokensForDisabledAccounts() {
        JwtService jwtService = jwtServiceWithSecret("segredo-de-teste-com-mais-de-32-caracteres-123");
        User user = User.builder().id(1L).email("aluno@email.com").role(Role.STUDENT).active(true).build();
        String token = jwtService.generateToken(user);
        user.setActive(false);
        assertFalse(jwtService.isTokenValid(token, new UserPrincipal(user)));
    }
}

