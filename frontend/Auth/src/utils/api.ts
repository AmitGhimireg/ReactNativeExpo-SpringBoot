import AsyncStorage from "@react-native-async-storage/async-storage";

// ─────────────────────────────────────────────────────────────────────────────
// api.ts — Central API utility for the React Native / Expo frontend.
//
// WHAT IT DOES:
//   • Stores / retrieves the access token and refresh token from AsyncStorage
//   • Provides apiRequest() — a fetch wrapper that attaches the access token
//     and automatically refreshes it when the server returns 401
//   • Exports authApi — typed wrappers for every auth endpoint
//
// HOW TOKENS ARE STORED (AsyncStorage):
//   "access_token"  → short-lived JWT (15 min)
//   "refresh_token" → long-lived UUID (7 days), used to renew the access token
//   "user_info"     → cached { email, fullName, emailVerified }
//
// REUSE: copy this file to any React Native project. Change BASE_URL to your
// backend address. The token refresh logic is generic.
// ─────────────────────────────────────────────────────────────────────────────

  // Change this to your machine's local IP when testing on a physical device.
  // Android emulator:   http://10.0.2.2:8080
  // iOS simulator:      http://localhost:8080
  // export const BASE_URL = "http://192.168.100.34:8080";
   export const BASE_URL = "http://localhost:8080";

const ACCESS_TOKEN_KEY  = "access_token";
const REFRESH_TOKEN_KEY = "refresh_token";  // ADDED
const USER_KEY          = "user_info";

// ── Access Token helpers ──────────────────────────────────────────────────────
export const saveAccessToken  = async (t: string) => AsyncStorage.setItem(ACCESS_TOKEN_KEY, t);
export const getAccessToken   = async (): Promise<string | null> => AsyncStorage.getItem(ACCESS_TOKEN_KEY);
export const removeAccessToken = async () => AsyncStorage.removeItem(ACCESS_TOKEN_KEY);

// ── Refresh Token helpers (ADDED) ─────────────────────────────────────────────
// The refresh token must be stored securely. In production use expo-secure-store.
export const saveRefreshToken  = async (t: string) => AsyncStorage.setItem(REFRESH_TOKEN_KEY, t);
export const getRefreshToken   = async (): Promise<string | null> => AsyncStorage.getItem(REFRESH_TOKEN_KEY);
export const removeRefreshToken = async () => AsyncStorage.removeItem(REFRESH_TOKEN_KEY);

// ── User info helpers ─────────────────────────────────────────────────────────
export const saveUser = async (user: { email: string; fullName: string; emailVerified: boolean }) =>
  AsyncStorage.setItem(USER_KEY, JSON.stringify(user));

export const getUser = async () => {
  const raw = await AsyncStorage.getItem(USER_KEY);
  return raw ? JSON.parse(raw) : null;
};

// ── Clear all auth data (called on logout) ────────────────────────────────────
export const clearAuthData = async () =>
  AsyncStorage.multiRemove([ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY, USER_KEY]);

// ── Token refresh (ADDED) ─────────────────────────────────────────────────────
/**
 * Calls POST /api/auth/refresh with the stored refresh token.
 * If successful, saves the new access token (and rotated refresh token).
 * Returns the new access token, or null if the refresh token is expired/invalid.
 */
const refreshAccessToken = async (): Promise<string | null> => {
  const refreshToken = await getRefreshToken();
  if (!refreshToken) return null;

  try {
    const res = await fetch(`${BASE_URL}/api/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) {
      // Refresh token is invalid or expired — force re-login
      await clearAuthData();
      return null;
    }
    const data = await res.json();
    // Save both the new access token and the rotated refresh token
    await saveAccessToken(data.accessToken);
    await saveRefreshToken(data.refreshToken);
    return data.accessToken;
  } catch {
    return null;
  }
};

// ── Generic fetch wrapper ─────────────────────────────────────────────────────
/**
 * Makes an HTTP request to the backend.
 *   • Attaches Authorization: Bearer <accessToken> when requireAuth=true
 *   • ADDED: On 401, tries refreshing the access token once and retries the request
 *   • Throws an Error with the server's message on failure
 */
export const apiRequest = async (
  endpoint: string,
  method: "GET" | "POST" | "PUT" | "DELETE" = "GET",
  body?: object,
  requireAuth = false
): Promise<any> => {
  const makeRequest = async (token?: string | null) => {
    const headers: Record<string, string> = { "Content-Type": "application/json" };
    if (requireAuth && token) headers["Authorization"] = `Bearer ${token}`;
    return fetch(`${BASE_URL}${endpoint}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });
  };

  let token = requireAuth ? await getAccessToken() : null;
  let response = await makeRequest(token);

  // ADDED: If 401 and we have a refresh token, try to renew the access token once
  if (response.status === 401 && requireAuth) {
    const newToken = await refreshAccessToken();
    if (newToken) {
      response = await makeRequest(newToken);  // retry with fresh token
    }
  }

  const data = await response.json();
  if (!response.ok) throw new Error(data.message || "Something went wrong");
  return data;
};

// ── Auth API typed wrappers ───────────────────────────────────────────────────
export const authApi = {
  /** POST /api/auth/register — Create account; returns accessToken + refreshToken */
  register: (fullName: string, email: string, password: string) =>
    apiRequest("/api/auth/register", "POST", { fullName, email, password }),

  /** POST /api/auth/login — Login; returns accessToken + refreshToken */
  login: (email: string, password: string) =>
    apiRequest("/api/auth/login", "POST", { email, password }),

  /** POST /api/auth/refresh — ADDED: Renew access token using refresh token */
  refresh: (refreshToken: string) =>
    apiRequest("/api/auth/refresh", "POST", { refreshToken }),

  /** POST /api/auth/logout — ADDED: Revoke refresh token server-side */
  logout: (refreshToken: string) =>
    apiRequest("/api/auth/logout", "POST", { refreshToken }),

  /** GET /api/auth/profile — Returns current user's profile (requires access token) */
  profile: () => apiRequest("/api/auth/profile", "GET", undefined, true),
};
