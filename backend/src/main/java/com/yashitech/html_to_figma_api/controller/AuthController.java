package com.yashitech.html_to_figma_api.controller;

import com.yashitech.html_to_figma_api.dto.AuthResponse;
import com.yashitech.html_to_figma_api.dto.LoginRequest;
import com.yashitech.html_to_figma_api.dto.RegisterRequest;
import com.yashitech.html_to_figma_api.entity.User;
import com.yashitech.html_to_figma_api.service.UserService;
import com.yashitech.html_to_figma_api.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AuthController.java — REST controller exposing all authentication endpoints.
 *
 * ENDPOINTS:
 *   POST /api/auth/register        → register new user (sends verification email)
 *   POST /api/auth/login           → login; returns accessToken + refreshToken
 *   POST /api/auth/refresh         → ADDED: exchange refreshToken for new accessToken
 *   GET  /api/auth/verify-email    → ADDED: click link from verification email
 *   POST /api/auth/logout          → ADDED: revoke refresh token in DB
 *   GET  /api/auth/profile         → protected; returns user data (requires valid accessToken)
 *
 * HOW TOKENS FLOW:
 *   1. Client calls /login → gets { accessToken, refreshToken }
 *   2. Client stores both; sends accessToken in Authorization header for API calls
 *   3. When accessToken expires (15 min), client calls /refresh with refreshToken
 *   4. Server validates refreshToken in DB → issues new accessToken
 *   5. On logout, client calls /logout → server deletes refreshToken from DB
 *
 * REUSE: all endpoints here are generic. Copy to any JWT Spring Boot project.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private UserService userService;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private AuthenticationManager authenticationManager;

    // ── POST /api/auth/register ───────────────────────────────────────────────
    /**
     * Creates a new user account.
     * MODIFIED: Now sends a verification email after saving the user.
     * Returns accessToken + refreshToken so the user can be auto-logged in
     * even before verifying their email (emailVerified: false in response).
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        // Basic validation
        if (req.getFullName() == null || req.getFullName().isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "Full name is required."));
        if (req.getEmail() == null || !req.getEmail().contains("@"))
            return ResponseEntity.badRequest().body(Map.of("message", "Valid email is required."));
        if (req.getPassword() == null || req.getPassword().length() < 6)
            return ResponseEntity.badRequest().body(Map.of("message", "Password must be at least 6 characters."));

        try {
            User saved = userService.registerUser(req);   // sends verification email

            // Issue both tokens after registration (auto-login)
            String accessToken  = jwtUtil.generateAccessToken(saved.getEmail());
            String refreshToken = jwtUtil.generateRefreshToken();
            userService.saveRefreshToken(saved, refreshToken);   // ADDED: persist refresh token

            return ResponseEntity.ok(new AuthResponse(
                accessToken, refreshToken,
                saved.getEmail(), saved.getFullName(),
                "Registration successful! Please check your email to verify your account.",
                saved.isEmailVerified()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── POST /api/auth/login ──────────────────────────────────────────────────
    /**
     * Authenticates the user and issues accessToken + refreshToken.
     * Spring Security's AuthenticationManager verifies the BCrypt password hash.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            // Let Spring Security validate email + password
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
            );
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password."));
        }

        User user = userService.findByEmail(req.getEmail());

        // Issue access token (JWT) + refresh token (UUID stored in DB)
        String accessToken  = jwtUtil.generateAccessToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken();
        userService.saveRefreshToken(user, refreshToken);   // ADDED: persist in DB

        return ResponseEntity.ok(new AuthResponse(
            accessToken, refreshToken,
            user.getEmail(), user.getFullName(),
            "Login successful!",
            user.isEmailVerified()
        ));
    }

    // ── POST /api/auth/refresh ────────────────────────────────────────────────
    /**
     * ADDED: Allows the client to get a new access token without re-logging in.
     *
     * Flow:
     *   1. Client sends: { "refreshToken": "<UUID>" }
     *   2. Server finds the user who owns that UUID in the DB
     *   3. Checks the refresh token hasn't expired
     *   4. Issues a new access token (and optionally rotates the refresh token)
     *
     * Why DB-backed instead of JWT? → We can revoke it on logout / password change.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String incomingRefreshToken = body.get("refreshToken");
        if (incomingRefreshToken == null || incomingRefreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Refresh token is required."));
        }

        try {
            User user = userService.findByRefreshToken(incomingRefreshToken);

            // Check refresh token expiry
            if (user.getRefreshTokenExpiry() == null ||
                    LocalDateTime.now().isAfter(user.getRefreshTokenExpiry())) {
                userService.clearRefreshToken(user);  // clean up expired token
                return ResponseEntity.status(401).body(Map.of("message", "Refresh token has expired. Please log in again."));
            }

            // Issue new access token
            String newAccessToken = jwtUtil.generateAccessToken(user.getEmail());

            // Rotate the refresh token (security best practice: one-time use)
            String newRefreshToken = jwtUtil.generateRefreshToken();
            userService.saveRefreshToken(user, newRefreshToken);

            return ResponseEntity.ok(Map.of(
                "accessToken",  newAccessToken,
                "refreshToken", newRefreshToken,   // client must update stored refresh token
                "message",      "Token refreshed successfully."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        }
    }

    // ── GET /api/auth/verify-email?token=... ─────────────────────────────────
    /**
     * ADDED: Endpoint hit when user clicks the link in the verification email.
     * Marks the user's email as verified in the DB.
     */
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            User user = userService.verifyEmail(token);
            return ResponseEntity.ok(Map.of(
                "message", "Email verified successfully! You can now use all features.",
                "email",   user.getEmail()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── POST /api/auth/logout ─────────────────────────────────────────────────
    /**
     * ADDED: Revokes the refresh token so it can never be used again.
     * The access token is short-lived (15 min) so it self-expires;
     * the client should also delete it from local storage.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                User user = userService.findByRefreshToken(refreshToken);
                userService.clearRefreshToken(user);  // delete from DB
            } catch (Exception ignored) {
                // Token already invalid — that's fine for logout
            }
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    // ── GET /api/auth/profile  (protected) ───────────────────────────────────
    /**
     * Returns the logged-in user's profile.
     * Requires a valid access token in Authorization: Bearer <token>.
     * JwtAuthFilter validates the token before this method is called.
     */
    @GetMapping("/profile")
    public ResponseEntity<?> profile(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        String email = jwtUtil.extractEmail(authHeader.substring(7));
        User user = userService.findByEmail(email);
        return ResponseEntity.ok(Map.of(
            "id",            user.getId(),
            "email",         user.getEmail(),
            "fullName",      user.getFullName(),
            "emailVerified", user.isEmailVerified()   // ADDED: frontend can show banner if false
        ));
    }
}
