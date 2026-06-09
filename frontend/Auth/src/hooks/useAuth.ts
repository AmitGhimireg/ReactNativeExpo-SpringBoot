import { useState, useEffect } from "react";
import {
  getAccessToken,
  getUser,
  clearAuthData,
  authApi,
  saveAccessToken,
  saveRefreshToken,
  saveUser,
  getRefreshToken,
} from "../utils/api";

/**
 * useAuth.ts — React hook that manages authentication state.
 *
 * WHAT IT RETURNS:
 *   • isLoggedIn       → null (loading) | true | false
 *   • user             → { email, fullName, emailVerified } or null
 *   • emailVerified    → quick access to verification status
 *   • logout()         → clears tokens locally AND revokes the refresh token server-side
 *
 * ADDED: Now tracks emailVerified so any screen can show a verification banner.
 *
 * REUSE: use this hook in any screen that needs to know the auth state.
 */
export function useAuth() {
  const [isLoggedIn, setIsLoggedIn] = useState<boolean | null>(null); // null = loading
  const [user, setUser] = useState<{
    email: string;
    fullName: string;
    emailVerified: boolean;   // ADDED
  } | null>(null);

  useEffect(() => {
    // On mount, check if a valid access token exists in local storage
    const check = async () => {
      const token    = await getAccessToken();
      const userData = await getUser();
      setIsLoggedIn(!!token);
      setUser(userData);
    };
    check();
  }, []);

  // ── Logout ────────────────────────────────────────────────────────────────
  /**
   * ADDED: Calls POST /api/auth/logout to revoke the refresh token in the DB,
   * then clears all local auth data.
   */
  const logout = async () => {
    try {
      const refreshToken = await getRefreshToken();
      if (refreshToken) await authApi.logout(refreshToken);  // server-side revocation
    } catch {
      // Network failure during logout is acceptable — still clear local data
    }
    await clearAuthData();
    setIsLoggedIn(false);
    setUser(null);
  };

  return { isLoggedIn, user, logout, emailVerified: user?.emailVerified ?? false };
}
