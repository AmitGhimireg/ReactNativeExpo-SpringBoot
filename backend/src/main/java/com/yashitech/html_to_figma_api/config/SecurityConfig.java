package com.yashitech.html_to_figma_api.config;

import com.yashitech.html_to_figma_api.filter.JwtAuthFilter;
import com.yashitech.html_to_figma_api.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * SecurityConfig.java — Spring Security configuration.
 *
 * KEY DECISIONS:
 *   • STATELESS session (no HttpSession) — JWT is the only auth mechanism.
 *   • CSRF disabled — not needed for stateless REST APIs.
 *   • CORS enabled — allows React Native / Expo to call from any origin (tighten in prod).
 *   • Public routes: /api/auth/register, /api/auth/login, /api/auth/refresh,
 *                    /api/auth/verify-email, /health
 *   • All other routes require a valid JWT access token.
 *
 * REUSE: copy to any JWT-secured Spring Boot project. Adjust the permitted
 * paths in authorizeHttpRequests() as needed.
 */
@Configuration
public class SecurityConfig {

    // @Lazy breaks the circular dependency:
    //   SecurityConfig → UserService → PasswordEncoder → SecurityConfig
    private final UserService userService;
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(@Lazy UserService userService,
                          @Lazy JwtAuthFilter jwtAuthFilter) {
        this.userService    = userService;
        this.jwtAuthFilter  = jwtAuthFilter;
    }

    // ── Password encoder (BCrypt) ─────────────────────────────────────────────
    // Used by UserService to hash passwords before saving, and by
    // DaoAuthenticationProvider to verify passwords during login.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ── Authentication provider ───────────────────────────────────────────────
    // Ties together UserDetailsService (UserService) + PasswordEncoder.
    // Spring Security calls this during login to verify credentials.
    @Bean
    @SuppressWarnings("deprecation")
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // ── AuthenticationManager ─────────────────────────────────────────────────
    // Exposed as a bean so AuthController can call authenticate() manually.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    // ── CORS — allow React Native / Expo / web clients ────────────────────────
    // TIGHTEN in production: replace "*" with your frontend domain.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOriginPatterns(List.of("*"));
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfig.setAllowedHeaders(List.of("*"));
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }

    // ── Security filter chain ─────────────────────────────────────────────────
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Allow cross-origin requests from the mobile app
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Disable CSRF — stateless JWT API doesn't need it
            .csrf(AbstractHttpConfigurer::disable)

            // No server-side session — every request must carry its own JWT
            .sessionManagement(sess ->
                sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── Route access rules ────────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // Public routes — no JWT needed
                .requestMatchers(
                    "/api/auth/register",       // create account
                    "/api/auth/login",          // get access + refresh tokens
                    "/api/auth/refresh",        // ADDED: renew access token with refresh token
                    "/api/auth/verify-email",   // ADDED: click link from email
                    "/health"
                ).permitAll()
                // ADDED: role-based route protection.
                // Authorities are loaded fresh from the DB on every request
                // (see UserService.loadUserByUsername), so a role change takes
                // effect immediately — no need to wait for the JWT to expire.
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/client/**").hasAnyRole("ADMIN", "CLIENT")
                // Everything else requires a valid access token
                .anyRequest().authenticated()
            )

            // Plug our JWT filter before the default username/password filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
