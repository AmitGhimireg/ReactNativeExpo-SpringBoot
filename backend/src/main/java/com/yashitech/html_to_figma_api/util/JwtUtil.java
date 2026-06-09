package com.yashitech.html_to_figma_api.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JwtUtil.java — Core JWT utility (Access Token + Refresh Token generation/validation).
 *
 * RESPONSIBILITIES:
 *   • generateAccessToken(email)  → short-lived signed JWT for API authentication
 *   • generateRefreshToken()      → random UUID saved in DB to renew sessions
 *   • extractEmail(token)         → read the subject claim from an access token
 *   • validateToken(token, user)  → verify signature + expiry + username
 *
 * HOW TOKENS DIFFER:
 *   Access Token  = signed JWT, stateless, 15-minute lifetime, sent in every request header
 *   Refresh Token = plain UUID, stored in DB (DB-backed = revocable), 7-day lifetime
 *
 * REUSE: drop this file into any Spring Boot project. Provide jwt.secret,
 * jwt.expiration, jwt.refresh-expiration in application.properties.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;          // access token lifetime (ms), default 900000 = 15 min

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;   // refresh token lifetime (ms), default 604800000 = 7 days

    // ── Signing key ───────────────────────────────────────────────────────────
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ── Access Token: Generate ────────────────────────────────────────────────
    /**
     * Creates a short-lived JWT access token.
     * The email is stored as the JWT "subject" claim.
     * Frontend sends: Authorization: Bearer <accessToken>
     */
    public String generateAccessToken(String email) {
        return Jwts.builder()
                .subject(email)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Refresh Token: Generate ───────────────────────────────────────────────
    /**
     * ADDED: Generates a random UUID to use as the refresh token.
     * This UUID is persisted in the users table (refresh_token column).
     * When the access token expires, the client sends this UUID to
     * POST /api/auth/refresh to receive a fresh access token.
     * Storing it in DB means we can revoke it (e.g., on logout or password change).
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    public long getRefreshExpiration() { return refreshExpiration; }

    // ── Extract email from JWT ────────────────────────────────────────────────
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    // ── Validate access token ─────────────────────────────────────────────────
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
