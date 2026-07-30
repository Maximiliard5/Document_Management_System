package com.example.dms.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates and validates JWT tokens using the HS256 algorithm.
 * The signing key and token TTL are injected from application configuration.
 */
@Service
public class JwtService {

    private final String secretKey;
    private final long expiration;

    public JwtService(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.expiration}") long expiration) {
        this.secretKey = secretKey;
        this.expiration = expiration;
    }

    /**
     * Creates a signed JWT with the given email as the subject and role as a custom claim.
     *
     * @param email the user's email address (used as the token subject)
     * @param role  the user's role name (stored as an informational claim)
     * @return a compact, signed JWT string
     */
    public String generateToken(String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the subject (email address) from a JWT.
     *
     * @param token the JWT string
     * @return the email address stored as the token subject
     * @throws io.jsonwebtoken.JwtException if the token is invalid or expired
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Returns {@code true} if the token's subject matches the given email and the token is not expired.
     *
     * @param token the JWT string to validate
     * @param email the expected email address
     * @return {@code true} if the token is valid for the given email
     */
    public boolean isTokenValid(String token, String email) {
        return extractEmail(token).equals(email) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}