package com.yashitech.html_to_figma_api.filter;

import com.yashitech.html_to_figma_api.service.UserService;
import com.yashitech.html_to_figma_api.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthFilter.java — Servlet filter that runs ONCE per request.
 *
 * WHAT IT DOES:
 *   1. Reads the "Authorization: Bearer <token>" header from every HTTP request.
 *   2. Extracts and validates the JWT access token.
 *   3. If valid, sets the authenticated user in Spring Security's SecurityContext.
 *   4. If invalid/missing, lets the request continue — Spring Security will reject
 *      it downstream for protected routes.
 *
 * POSITION IN THE CHAIN: placed BEFORE UsernamePasswordAuthenticationFilter
 * (configured in SecurityConfig).
 *
 * REUSE: copy as-is to any Spring Boot + JWT project.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Step 1: Read the Authorization header
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Step 2: Strip "Bearer " prefix to get the raw JWT
            String token = authHeader.substring(7);
            try {
                // Step 3: Extract the email claim from the token
                String email = jwtUtil.extractEmail(token);

                // Step 4: Only authenticate if not already authenticated
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userService.loadUserByUsername(email);

                    // Step 5: Validate signature + expiry
                    if (jwtUtil.validateToken(token, userDetails)) {
                        // Step 6: Build Spring Security auth object and set in context
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (Exception ignored) {
                // Bad/expired token — do not set auth; SecurityConfig will reject the request
            }
        }

        // Always continue the filter chain
        filterChain.doFilter(request, response);
    }
}
