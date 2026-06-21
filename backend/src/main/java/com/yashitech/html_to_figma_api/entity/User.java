package com.yashitech.html_to_figma_api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * User.java — JPA Entity mapped to the "users" table in PostgreSQL.
 *
 * FIELDS ADDED for full JWT auth:
 *   • emailVerified      → false until user clicks the verification link
 *   • emailVerifyToken   → UUID stored in DB, embedded in the verification email
 *   • emailVerifyExpiry  → when the verification link expires
 *   • refreshToken       → current valid refresh token (UUID, stored hashed in prod)
 *   • refreshTokenExpiry → when the refresh token expires
 *
 * Reuse note: copy this entity to any Spring Boot project. Just update the
 * package name and datasource config in application.properties.
 */
@Entity
@Table(name = "users")
public class User {

    // ── Primary key (auto-increment) ──────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Basic profile ─────────────────────────────────────────────────────────
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100)
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6)
    @Column(nullable = false)
    private String password;   // BCrypt-hashed; never stored in plain text

    // ── Email Verification Token ──────────────────────────────────────────────
    // ADDED: emailVerified tracks whether the user has confirmed their email.
    // emailVerifyToken is a UUID sent in the registration email.
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "email_verify_token")
    private String emailVerifyToken;   // UUID — lives in the verification link

    @Column(name = "email_verify_expiry")
    private LocalDateTime emailVerifyExpiry;  // link expires after 24 h

    // ── Refresh Token ─────────────────────────────────────────────────────────
    // ADDED: One refresh token per user stored in DB so we can revoke it on logout.
    @Column(name = "refresh_token")
    private String refreshToken;       // UUID (in production: store BCrypt hash)

    @Column(name = "refresh_token_expiry")
    private LocalDateTime refreshTokenExpiry;

    // ── Role (ADDED for role-based access control) ──────────────────────────────
    // Every new registration defaults to CLIENT (set explicitly in UserService).
    // Promote a user to ADMIN manually in the database:
    //   UPDATE users SET role = 'ADMIN' WHERE email = '...';
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.CLIENT;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public String getFullName()                { return fullName; }
    public void setFullName(String n)          { this.fullName = n; }

    public String getEmail()                   { return email; }
    public void setEmail(String e)             { this.email = e; }

    public String getPassword()                { return password; }
    public void setPassword(String p)          { this.password = p; }

    public boolean isEmailVerified()           { return emailVerified; }
    public void setEmailVerified(boolean v)    { this.emailVerified = v; }

    public String getEmailVerifyToken()        { return emailVerifyToken; }
    public void setEmailVerifyToken(String t)  { this.emailVerifyToken = t; }

    public LocalDateTime getEmailVerifyExpiry()          { return emailVerifyExpiry; }
    public void setEmailVerifyExpiry(LocalDateTime d)    { this.emailVerifyExpiry = d; }

    public String getRefreshToken()            { return refreshToken; }
    public void setRefreshToken(String t)      { this.refreshToken = t; }

    public LocalDateTime getRefreshTokenExpiry()         { return refreshTokenExpiry; }
    public void setRefreshTokenExpiry(LocalDateTime d)   { this.refreshTokenExpiry = d; }

    public Role getRole()                      { return role; }
    public void setRole(Role role)             { this.role = role; }
}
