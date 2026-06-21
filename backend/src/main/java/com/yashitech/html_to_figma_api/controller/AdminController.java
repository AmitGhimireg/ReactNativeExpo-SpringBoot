package com.yashitech.html_to_figma_api.controller;

import com.yashitech.html_to_figma_api.entity.Role;
import com.yashitech.html_to_figma_api.entity.User;
import com.yashitech.html_to_figma_api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AdminController.java — Endpoints reachable ONLY by users with ROLE_ADMIN.
 *
 * Access is enforced centrally in SecurityConfig:
 *   .requestMatchers("/api/admin/**").hasRole("ADMIN")
 *
 * If a CLIENT (or anyone without a valid token) calls these, Spring Security
 * rejects the request with 403 Forbidden before it ever reaches this class —
 * there's no need to re-check the role here.
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin("*")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Quick endpoint the frontend can call to confirm admin access is working
    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() {
        return ResponseEntity.ok(Map.of("message", "Welcome, Admin!"));
    }

    // Example admin-only resource: list every registered user + their role
    @GetMapping("/users")
    public ResponseEntity<?> listUsers() {
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "fullName", u.getFullName(),
                        "email", u.getEmail(),
                        "role", u.getRole().name(),
                        "emailVerified", u.isEmailVerified()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    // Example admin-only action: promote/demote a user's role
    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String requestedRole = body.get("role");
        try {
            user.setRole(Role.valueOf(requestedRole.toUpperCase()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid role. Use ADMIN or CLIENT."));
        }
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Role updated",
                "email", user.getEmail(),
                "role", user.getRole().name()));
    }
}
