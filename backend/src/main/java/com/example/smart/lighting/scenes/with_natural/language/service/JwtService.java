package com.example.smart.lighting.scenes.with_natural.language.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT service for generating and validating tokens.
 * Used for cross-domain OAuth authentication on Render deployment.
 */
@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600000}")
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        // Ensure key is at least 256 bits (32 bytes) for HS256
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
            keyBytes = paddedKey;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate a JWT token for a user.
     */
    public String generateToken(String userId, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("role", role);

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Validate and parse a JWT token.
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract user ID from token.
     */
    public String getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        return claims != null ? claims.get("userId", String.class) : null;
    }

    /**
     * Extract email from token.
     */
    public String getEmailFromToken(String token) {
        Claims claims = validateToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * Extract role from token.
     */
    public String getRoleFromToken(String token) {
        Claims claims = validateToken(token);
        return claims != null ? claims.get("role", String.class) : null;
    }

    /**
     * Check if token is expired.
     */
    public boolean isTokenExpired(String token) {
        Claims claims = validateToken(token);
        return claims == null || claims.getExpiration().before(new Date());
    }
}
