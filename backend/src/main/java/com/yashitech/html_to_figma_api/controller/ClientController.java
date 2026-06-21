package com.yashitech.html_to_figma_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ClientController.java — Endpoints reachable by BOTH ADMIN and CLIENT roles.
 *
 * Access is enforced centrally in SecurityConfig:
 *   .requestMatchers("/api/client/**").hasAnyRole("ADMIN", "CLIENT")
 */
@RestController
@RequestMapping("/api/client")
@CrossOrigin("*")
public class ClientController {

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() {
        return ResponseEntity.ok(Map.of("message", "Welcome, Client!"));
    }
}
