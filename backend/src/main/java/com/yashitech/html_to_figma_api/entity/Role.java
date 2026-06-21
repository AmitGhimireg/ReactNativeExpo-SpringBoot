package com.yashitech.html_to_figma_api.entity;

/**
 * Role.java — The two roles supported by this app.
 *
 * New registrations always default to CLIENT (see UserService.registerUser).
 * To make someone an ADMIN, update their row directly in the database:
 *
 *   UPDATE users SET role = 'ADMIN' WHERE email = 'youradmin@example.com';
 *
 * REUSE: add more values here (e.g. MANAGER) if you need additional roles later.
 */
public enum Role {
    ADMIN,
    CLIENT
}
