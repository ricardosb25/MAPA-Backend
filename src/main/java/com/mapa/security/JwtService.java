package com.mapa.security;

import com.mapa.config.properties.JwtProperties;
import com.mapa.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    public String generateToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getExpirationMs());
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("userId", user.getId())
                .claim("credentialsVersion", user.getCredentialsVersion())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(resolveSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().after(new Date())
                    && claims.get("userId", Long.class) != null
                    && claims.get("credentialsVersion", Long.class) != null;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean isTokenValid(String token, UserPrincipal principal) {
        if (!principal.isEnabled()) {
            return false;
        }
        try {
            Claims claims = extractClaims(token);
            Long userId = claims.get("userId", Long.class);
            Long credentialsVersion = claims.get("credentialsVersion", Long.class);
            return claims.getExpiration().after(new Date())
                    && principal.getUsername().equals(claims.getSubject())
                    && principal.getId().equals(userId)
                    && credentialsVersion != null
                    && credentialsVersion == principal.getCredentialsVersion();
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(resolveSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey resolveSigningKey() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET não configurado ou com menos de 32 caracteres");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}

