package com.yashitech.html_to_figma_api.dto;

/**
 * LoginRequest.java — Request body for POST /api/auth/login.
 * Frontend sends: { "email": "...", "password": "..." }
 */
public class LoginRequest {
    private String email;
    private String password;

    public String getEmail()            { return email; }
    public void setEmail(String e)      { this.email = e; }
    public String getPassword()         { return password; }
    public void setPassword(String p)   { this.password = p; }
}
