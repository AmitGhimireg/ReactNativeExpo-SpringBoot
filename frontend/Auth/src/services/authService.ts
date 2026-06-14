import axios from "axios";

// Change this to your backend URL
const API_URL = "http://localhost:8080/api/auth";

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  email: string;
  fullName: string;
  message: string;
  emailVerified: boolean;
}

// ─────────────────────────────────────────────
// Axios instance
// ─────────────────────────────────────────────
const api = axios.create({
  baseURL: API_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// ─────────────────────────────────────────────
// Auth API
// ─────────────────────────────────────────────
export const authService = {
  // REGISTER
  register: async (data: RegisterRequest) => {
    const res = await api.post("/register", data);
    return res.data;
  },

  // LOGIN
  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const res = await api.post<AuthResponse>("/login", data);
    return res.data;
  },

  // REFRESH TOKEN
  refreshToken: async (refreshToken: string) => {
    const res = await api.post("/refresh", {
      refreshToken,
    });
    return res.data;
  },

  // GET PROFILE (requires access token)
  getProfile: async (accessToken: string) => {
    const res = await api.get("/profile", {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });
    return res.data;
  },

  // LOGOUT
  logout: async (refreshToken: string) => {
    const res = await api.post("/logout", {
      refreshToken,
    });
    return res.data;
  },
};

export default api;