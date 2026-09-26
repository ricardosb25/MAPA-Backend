package com.mapa.security;

import com.mapa.config.properties.JwtProperties;
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

        String token = jwtService.generateToken("aluno@email.com", "STUDENT");

        assertTrue(jwtService.isTokenValid(token));
        assertEquals("aluno@email.com", jwtService.extractEmail(token));
    }

    @Test
    void shouldRejectInvalidToken() {
        JwtService jwtService = jwtServiceWithSecret("segredo-de-teste-com-mais-de-32-caracteres-123");

        assertFalse(jwtService.isTokenValid("token.invalido.aqui"));
    }
}

