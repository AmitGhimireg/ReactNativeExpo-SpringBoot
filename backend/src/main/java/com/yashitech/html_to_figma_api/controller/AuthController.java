package com.yashitech.html_to_figma_api.controller;

import com.yashitech.html_to_figma_api.dto.LoginRequest;
import com.yashitech.html_to_figma_api.dto.RegisterRequest;
import com.yashitech.html_to_figma_api.entity.User;
import com.yashitech.html_to_figma_api.service.UserService;
import com.yashitech.html_to_figma_api.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService, JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    // REGISTER
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {

        if (req.getEmail() == null || req.getPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid data"));
        }

        userService.registerUser(req); // sends email verification

        return ResponseEntity.ok(Map.of(
                "message", "Registration successful. Please verify your email."));
    }

    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Invalid credentials"));
        }

        User user = userService.findByEmail(req.getEmail());

        // ❌ IMPORTANT FIX: block unverified users
        if (!user.isEmailVerified()) {
            return ResponseEntity.status(403)
                    .body(Map.of(
                            "message", "Please verify your email before login",
                            "emailVerified", false));
        }

        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken();

        userService.saveRefreshToken(user, refreshToken);

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "role", user.getRole().name(),
                "emailVerified", user.isEmailVerified()));
    }

    // REFRESH TOKEN
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {

        String refreshToken = body.get("refreshToken");

        User user = userService.findByRefreshToken(refreshToken);

        if (user.getRefreshTokenExpiry() == null ||
                LocalDateTime.now().isAfter(user.getRefreshTokenExpiry())) {
            userService.clearRefreshToken(user);
            return ResponseEntity.status(401).body(Map.of("message", "Expired refresh token"));
        }

        String newAccessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole().name());
        String newRefreshToken = jwtUtil.generateRefreshToken();

        userService.saveRefreshToken(user, newRefreshToken);

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken,
                "role", user.getRole().name()));
    }

    // VERIFY EMAIL
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        User user = userService.verifyEmail(token);

        return ResponseEntity.ok(Map.of(
                "message", "Email verified successfully",
                "email", user.getEmail()));
    }

    // LOGOUT
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {

        String refreshToken = body.get("refreshToken");

        if (refreshToken != null) {
            try {
                User user = userService.findByRefreshToken(refreshToken);
                userService.clearRefreshToken(user);
            } catch (Exception ignored) {
            }
        }

        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    // PROFILE
    @GetMapping("/profile")
    public ResponseEntity<?> profile(@RequestHeader("Authorization") String auth) {

        String email = jwtUtil.extractEmail(auth.substring(7));
        User user = userService.findByEmail(email);

        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "role", user.getRole().name(),
                "emailVerified", user.isEmailVerified()));
    }
}