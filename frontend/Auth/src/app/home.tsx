import React, { useEffect, useState } from "react";
import {
  View, Text, TouchableOpacity, StyleSheet,
  ActivityIndicator, Alert,
} from "react-native";
import { router } from "expo-router";
import { authApi, getUser, clearAuthData, getRefreshToken } from "../utils/api";

/**
 * home.tsx — Protected home screen. Only accessible with a valid access token.
 *
 * FLOW:
 *   1. On mount, loads cached user from AsyncStorage (instant display).
 *   2. Then fetches fresh profile from GET /api/auth/profile (uses access token).
 *      → api.ts auto-refreshes the access token if it gets a 401.
 *   3. If token refresh also fails → clears all auth data → redirects to /login.
 *
 * ADDED: Shows an email verification banner if emailVerified=false.
 * ADDED: Logout now calls POST /api/auth/logout to revoke the refresh token server-side.
 */
export default function Home() {
  const [user, setUser] = useState<{
    fullName: string;
    email: string;
    emailVerified: boolean;   // ADDED
  } | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      // Show cached data immediately while we fetch fresh data
      const cached = await getUser();
      if (cached) setUser(cached);

      // Fetch live profile — access token is attached by apiRequest();
      // if it's expired, apiRequest() will use the refresh token automatically.
      const profile = await authApi.profile();
      setUser({
        fullName:      profile.fullName,
        email:         profile.email,
        emailVerified: profile.emailVerified,  // ADDED
      });
    } catch {
      // Both access token and refresh token failed → force re-login
      await clearAuthData();
      router.replace("/login");
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    Alert.alert("Logout", "Are you sure you want to logout?", [
      { text: "Cancel", style: "cancel" },
      {
        text: "Logout",
        style: "destructive",
        onPress: async () => {
          // ADDED: Revoke refresh token server-side so it can't be reused
          try {
            const refreshToken = await getRefreshToken();
            if (refreshToken) await authApi.logout(refreshToken);
          } catch { /* network error on logout is acceptable */ }
          await clearAuthData();
          router.replace("/login");
        },
      },
    ]);
  };

  if (loading) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color="#4CAF50" />
        <Text style={styles.loadingText}>Loading profile…</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerText}>🏠 Home</Text>
      </View>

      {/* ADDED: Email verification banner */}
      {user && !user.emailVerified && (
        <View style={styles.verifyBanner}>
          <Text style={styles.verifyBannerText}>
            📧 Please check your email and verify your account.
          </Text>
        </View>
      )}

      {/* Welcome card */}
      <View style={styles.card}>
        <View style={styles.avatar}>
          <Text style={styles.avatarText}>
            {user?.fullName?.charAt(0).toUpperCase() ?? "?"}
          </Text>
        </View>
        <Text style={styles.welcomeTitle}>Welcome back!</Text>
        <Text style={styles.userName}>{user?.fullName}</Text>
        <Text style={styles.userEmail}>{user?.email}</Text>
        {user?.emailVerified && (
          <Text style={styles.verifiedBadge}>✅ Email Verified</Text>
        )}
      </View>

      {/* Auth info box */}
      <View style={styles.infoBox}>
        <Text style={styles.infoTitle}>🔐 JWT Authentication Active</Text>
        <Text style={styles.infoText}>
          Access Token: short-lived JWT (15 min) — sent in every API request.{"\n"}
          Refresh Token: long-lived UUID (7 days) — silently renews the access token.{"\n"}
          Email Verification: UUID link sent to your inbox on registration.
        </Text>
      </View>

      {/* Logout */}
      <TouchableOpacity style={styles.logoutBtn} onPress={handleLogout}>
        <Text style={styles.logoutText}>Logout</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container:       { flex: 1, backgroundColor: "#f9f9f9", padding: 20, paddingTop: 60 },
  centered:        { flex: 1, justifyContent: "center", alignItems: "center" },
  loadingText:     { marginTop: 12, color: "#888" },
  header:          { marginBottom: 16 },
  headerText:      { fontSize: 22, fontWeight: "bold", color: "#1a1a1a" },
  // ADDED: email verification banner styles
  verifyBanner:    { backgroundColor: "#fff3cd", borderRadius: 10, padding: 12, marginBottom: 16, borderLeftWidth: 4, borderLeftColor: "#f0ad4e" },
  verifyBannerText:{ color: "#856404", fontSize: 13, lineHeight: 20 },
  card:            { backgroundColor: "#fff", borderRadius: 16, padding: 24, alignItems: "center", shadowColor: "#000", shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.08, shadowRadius: 6, elevation: 3, marginBottom: 20 },
  avatar:          { width: 72, height: 72, borderRadius: 36, backgroundColor: "#4CAF50", justifyContent: "center", alignItems: "center", marginBottom: 16 },
  avatarText:      { color: "#fff", fontSize: 28, fontWeight: "bold" },
  welcomeTitle:    { fontSize: 16, color: "#888", marginBottom: 4 },
  userName:        { fontSize: 22, fontWeight: "bold", color: "#1a1a1a", marginBottom: 4 },
  userEmail:       { fontSize: 14, color: "#888" },
  verifiedBadge:   { marginTop: 8, fontSize: 13, color: "#27ae60", fontWeight: "600" },
  infoBox:         { backgroundColor: "#e8f5e9", borderRadius: 12, padding: 16, marginBottom: 30 },
  infoTitle:       { fontWeight: "bold", fontSize: 14, color: "#2e7d32", marginBottom: 6 },
  infoText:        { fontSize: 12, color: "#388e3c", lineHeight: 20 },
  logoutBtn:       { backgroundColor: "#f44336", padding: 15, borderRadius: 10, alignItems: "center" },
  logoutText:      { color: "#fff", fontWeight: "bold", fontSize: 16 },
});
