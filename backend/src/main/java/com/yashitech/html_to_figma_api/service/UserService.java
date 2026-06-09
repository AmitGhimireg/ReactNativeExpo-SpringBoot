package com.yashitech.html_to_figma_api.service;

import com.yashitech.html_to_figma_api.dto.RegisterRequest;
import com.yashitech.html_to_figma_api.entity.User;
import com.yashitech.html_to_figma_api.repository.UserRepository;
import com.yashitech.html_to_figma_api.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

/**
 * UserService.java — Business logic for user registration, verification, and token management.
 *
 * IMPLEMENTS UserDetailsService so Spring Security can load a user during login.
 *
 * METHODS ADDED for full JWT auth:
 *   • registerUser(req)               → saves user, generates email-verify token, sends email
 *   • verifyEmail(token)              → marks user verified when link is clicked
 *   • saveRefreshToken(user, token)   → persists refresh token + expiry in DB
 *   • findByRefreshToken(token)       → looks up user by their refresh UUID
 *   • clearRefreshToken(user)         → called on logout to revoke the token
 *
 * REUSE: copy this file. Update the package name and make sure EmailService,
 * JwtUtil, UserRepository, and PasswordEncoder are in the same project.
 */
@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;   // ADDED: for sending verification email

    @Autowired
    private JwtUtil jwtUtil;             // ADDED: to get refresh token expiry duration

    @Value("${email.verification.expiry-hours:24}")
    private int verifyExpiryHours;

    // ── Called by Spring Security (and JwtAuthFilter) to load user details ─────
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account found: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.emptyList()
        );
    }

    // ── Registration ──────────────────────────────────────────────────────────
    /**
     * MODIFIED: Now also generates an email-verification token (UUID) and
     * calls EmailService to send the verification link.
     * The user is saved with emailVerified=false until they click the link.
     */
    public User registerUser(RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("An account with this email already exists.");
        }

        User user = new User();
        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));

        // ADDED: Generate and store email verification token
        String verifyToken = UUID.randomUUID().toString();
        user.setEmailVerifyToken(verifyToken);
        user.setEmailVerifyExpiry(LocalDateTime.now().plusHours(verifyExpiryHours));
        user.setEmailVerified(false);  // must click link to verify

        User saved = userRepository.save(user);

        // ADDED: Send verification email (fire-and-forget; log error but don't break registration)
        try {
            emailService.sendVerificationEmail(saved.getEmail(), verifyToken);
        } catch (Exception e) {
            // Log in production; don't fail registration if mail server is down
            System.err.println("Warning: Could not send verification email: " + e.getMessage());
        }

        return saved;
    }

    // ── Email Verification ────────────────────────────────────────────────────
    /**
     * ADDED: Called by GET /api/auth/verify-email?token=...
     * Finds the user by the UUID in the link, checks expiry, marks email as verified,
     * and clears the token so the link can't be reused.
     */
    public User verifyEmail(String token) {
        User user = userRepository.findByEmailVerifyToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token."));

        if (user.getEmailVerifyExpiry() == null ||
                LocalDateTime.now().isAfter(user.getEmailVerifyExpiry())) {
            throw new RuntimeException("Verification link has expired. Please request a new one.");
        }

        user.setEmailVerified(true);
        user.setEmailVerifyToken(null);   // invalidate token so link can't be reused
        user.setEmailVerifyExpiry(null);
        return userRepository.save(user);
    }

    // ── Refresh Token management ──────────────────────────────────────────────
    /**
     * ADDED: Saves a new refresh token UUID + expiry to the user's DB row.
     * Called after successful login and after refreshing the access token.
     */
    public void saveRefreshToken(User user, String refreshToken) {
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(
            LocalDateTime.now().plusSeconds(jwtUtil.getRefreshExpiration() / 1000)
        );
        userRepository.save(user);
    }

    /** ADDED: Looks up a user by their refresh token UUID (used by /refresh endpoint). */
    public User findByRefreshToken(String token) {
        return userRepository.findByRefreshToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired refresh token."));
    }

    /** ADDED: Clears the refresh token from DB (called on logout to invalidate the session). */
    public void clearRefreshToken(User user) {
        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);
        userRepository.save(user);
    }

    // ── Utility ───────────────────────────────────────────────────────────────
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
