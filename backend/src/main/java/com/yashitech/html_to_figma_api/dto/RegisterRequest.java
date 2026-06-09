package com.yashitech.html_to_figma_api.dto;

/**
 * RegisterRequest.java — Request body for POST /api/auth/register.
 * Frontend sends: { "fullName": "...", "email": "...", "password": "..." }
 * REUSE: copy to any project that has a registration endpoint.
 */
public class RegisterRequest {
    private String fullName;
    private String email;
    private String password;

    public String getFullName()         { return fullName; }
    public void setFullName(String n)   { this.fullName = n; }
    public String getEmail()            { return email; }
    public void setEmail(String e)      { this.email = e; }
    public String getPassword()         { return password; }
    public void setPassword(String p)   { this.password = p; }
}
