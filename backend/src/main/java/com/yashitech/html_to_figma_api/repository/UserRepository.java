package com.yashitech.html_to_figma_api.repository;

import com.yashitech.html_to_figma_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * UserRepository.java — Spring Data JPA repository for the User entity.
 *
 * Spring auto-generates all SQL at runtime; no implementation file needed.
 *
 * METHODS ADDED for new auth features:
 *   • findByRefreshToken(token) → lets the refresh endpoint look up the user
 *                                 who owns a given refresh UUID
 *   • findByEmailVerifyToken(token) → lets the verify-email endpoint find the
 *                                     user who owns a given verification UUID
 *
 * REUSE: copy to any Spring Boot project — just update the entity type and package.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // Used during login and JWT validation to look up a user by their email
    Optional<User> findByEmail(String email);

    // ADDED: Used by POST /api/auth/refresh to look up user by their refresh token UUID
    Optional<User> findByRefreshToken(String refreshToken);

    // ADDED: Used by GET /api/auth/verify-email?token=... to find user and mark email verified
    Optional<User> findByEmailVerifyToken(String emailVerifyToken);
}
