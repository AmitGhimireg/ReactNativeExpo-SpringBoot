package com.yashitech.html_to_figma_api.dto;

/**
 * AuthResponse.java — Response body returned by login and register endpoints.
 *
 * MODIFIED: Now includes refreshToken field.
 *   • accessToken   → short-lived JWT (15 min); send in Authorization: Bearer header
 *   • refreshToken  → long-lived UUID (7 days); store securely (e.g. AsyncStorage / Keychain)
 *   • email         → for the frontend to display user info
 *   • fullName      → for the frontend to display user info
 *   • message       → human-readable status message
 *   • emailVerified → ADDED: tells the frontend whether to show a "please verify email" banner
 *
 * REUSE: extend/reduce fields as needed for your project.
 */
public class AuthResponse {
    private String accessToken;
    private String refreshToken;    // ADDED
    private String email;
    private String fullName;
    private String role;            // ADDED: "ADMIN" or "CLIENT"
    private String message;
    private boolean emailVerified;  // ADDED

    public AuthResponse() {}

    public AuthResponse(String accessToken, String refreshToken,
                        String email, String fullName, String role,
                        String message, boolean emailVerified) {
        this.accessToken   = accessToken;
        this.refreshToken  = refreshToken;
        this.email         = email;
        this.fullName      = fullName;
        this.role          = role;
        this.message       = message;
        this.emailVerified = emailVerified;
    }

    public String getAccessToken()              { return accessToken; }
    public void setAccessToken(String t)        { this.accessToken = t; }
    public String getRefreshToken()             { return refreshToken; }
    public void setRefreshToken(String t)       { this.refreshToken = t; }
    public String getEmail()                    { return email; }
    public void setEmail(String e)              { this.email = e; }
    public String getFullName()                 { return fullName; }
    public void setFullName(String n)           { this.fullName = n; }
    public String getRole()                     { return role; }
    public void setRole(String r)               { this.role = r; }
    public String getMessage()                  { return message; }
    public void setMessage(String m)            { this.message = m; }
    public boolean isEmailVerified()            { return emailVerified; }
    public void setEmailVerified(boolean v)     { this.emailVerified = v; }
}
