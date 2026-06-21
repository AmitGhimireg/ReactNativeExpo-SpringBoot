import AsyncStorage from "@react-native-async-storage/async-storage";

export const BASE_URL = "http://192.168.100.34:8080";

// Storage Keys
const ACCESS_TOKEN = "access_token";
const REFRESH_TOKEN = "refresh_token";
const USER_KEY = "user";

/* ─────────────────────────────
   TOKEN STORAGE
──────────────────────────── */

export const saveAccessToken = (t?: string) =>
  t ? AsyncStorage.setItem(ACCESS_TOKEN, t) : null;

export const saveRefreshToken = (t?: string) =>
  t ? AsyncStorage.setItem(REFRESH_TOKEN, t) : null;

export const getAccessToken = () => AsyncStorage.getItem(ACCESS_TOKEN);

export const getRefreshToken = () => AsyncStorage.getItem(REFRESH_TOKEN);

export const saveUser = (user: any) =>
  AsyncStorage.setItem(USER_KEY, JSON.stringify(user));

export const getUser = async () => {
  const u = await AsyncStorage.getItem(USER_KEY);
  return u ? JSON.parse(u) : null;
};

export const clearAuth = async () => {
  await AsyncStorage.multiRemove([ACCESS_TOKEN, REFRESH_TOKEN, USER_KEY]);
};

/* ─────────────────────────────
   REFRESH TOKEN FLOW
──────────────────────────── */

const refreshTokenRequest = async () => {
  const token = await getRefreshToken();
  if (!token) return null;

  const res = await fetch(`${BASE_URL}/api/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken: token }),
  });

  if (!res.ok) {
    await clearAuth();
    return null;
  }

  const data = await res.json();

  await saveAccessToken(data.accessToken);
  await saveRefreshToken(data.refreshToken);

  return data.accessToken;
};

/* ─────────────────────────────
   MAIN API HANDLER
──────────────────────────── */
export const api = async (
  endpoint: string,
  method: string = "GET",
  body?: any,
  auth: boolean = false
) => {
  let token = auth ? await getAccessToken() : null;

  const makeRequest = async (t?: string | null) => {
    return fetch(BASE_URL + endpoint, {
      method,
      headers: {
        "Content-Type": "application/json",
        ...(auth && t ? { Authorization: `Bearer ${t}` } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
    });
  };

  let res = await makeRequest(token);

  // 🔥 READ ONLY ONCE
  const text = await res.text();

  let data;
  try {
    data = JSON.parse(text);
  } catch {
    data = text;
  }

  // 🔁 HANDLE 401 REFRESH
  if (res.status === 401 && auth) {
    const newToken = await refreshTokenRequest();

    if (newToken) {
      res = await makeRequest(newToken);

      const retryText = await res.text();
      try {
        return JSON.parse(retryText);
      } catch {
        return retryText;
      }
    }
  }

  if (!res.ok) {
    throw new Error(
      typeof data === "string" ? data : data.message || "Request failed"
    );
  }

  return data;
};

/* ─────────────────────────────
   AUTH API WRAPPER
──────────────────────────── */

export const authApi = {
  register: (fullName: string, email: string, password: string) =>
    api("/api/auth/register", "POST", { fullName, email, password }),

  login: (email: string, password: string) =>
    api("/api/auth/login", "POST", { email, password }),

  logout: (refreshToken: string) =>
    api("/api/auth/logout", "POST", { refreshToken }),

  profile: () => api("/api/auth/profile", "GET", null, true),

  // ADDED: role-protected endpoints (see SecurityConfig on the backend)
  adminDashboard: () => api("/api/admin/dashboard", "GET", null, true),
  adminUsers: () => api("/api/admin/users", "GET", null, true),
  clientDashboard: () => api("/api/client/dashboard", "GET", null, true),
};