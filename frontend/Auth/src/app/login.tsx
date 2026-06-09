import React, { useState } from "react";
import {
  View, Text, TextInput, TouchableOpacity,
  StyleSheet, ActivityIndicator, KeyboardAvoidingView,
  Platform, ScrollView,
} from "react-native";
import { router } from "expo-router";
import { authApi, saveAccessToken, saveRefreshToken, saveUser } from "../utils/api";

/**
 * login.tsx — Login screen.
 *
 * FLOW:
 *   1. User enters email + password.
 *   2. Calls POST /api/auth/login via authApi.login().
 *   3. On success: saves both accessToken AND refreshToken to AsyncStorage.
 *   4. Navigates to /home.
 *
 * MODIFIED: Now also saves the refreshToken so the app can silently renew
 * the access token when it expires (no re-login required for 7 days).
 */
export default function Login() {
  const [email, setEmail]       = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState("");

  const handleLogin = async () => {
    setError("");

    // Client-side validation
    if (!email.trim() || !password.trim()) {
      setError("Email and password are required.");
      return;
    }
    if (!email.includes("@")) {
      setError("Please enter a valid email address.");
      return;
    }

    setLoading(true);
    try {
      const data = await authApi.login(email.trim().toLowerCase(), password);

      // Save access token (JWT) — used in Authorization header for API calls
      await saveAccessToken(data.accessToken);

      // ADDED: Save refresh token (UUID) — used to renew the access token silently
      await saveRefreshToken(data.refreshToken);

      // Cache user info for the home screen
      await saveUser({
        email:         data.email,
        fullName:      data.fullName,
        emailVerified: data.emailVerified,  // ADDED
      });

      router.replace("/home");
    } catch (err: any) {
      setError(err.message || "Login failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={{ flex: 1 }}
      behavior={Platform.OS === "ios" ? "padding" : undefined}
    >
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <Text style={styles.title}>Welcome Back</Text>
        <Text style={styles.subtitle}>Sign in to your account</Text>

        {!!error && (
          <View style={styles.errorBox}>
            <Text style={styles.errorText}>{error}</Text>
          </View>
        )}

        <Text style={styles.label}>Email</Text>
        <TextInput
          placeholder="Enter your email"
          style={styles.input}
          value={email}
          onChangeText={setEmail}
          autoCapitalize="none"
          keyboardType="email-address"
          autoComplete="email"
        />

        <Text style={styles.label}>Password</Text>
        <TextInput
          placeholder="Enter your password"
          secureTextEntry
          style={styles.input}
          value={password}
          onChangeText={setPassword}
          autoComplete="password"
        />

        <TouchableOpacity
          style={[styles.button, loading && styles.buttonDisabled]}
          onPress={handleLogin}
          disabled={loading}
        >
          {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.btnText}>Login</Text>}
        </TouchableOpacity>

        <TouchableOpacity onPress={() => router.push("/register")}>
          <Text style={styles.link}>
            Don't have an account?{" "}
            <Text style={styles.linkBold}>Register</Text>
          </Text>
        </TouchableOpacity>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container:      { flexGrow: 1, justifyContent: "center", padding: 24, backgroundColor: "#f9f9f9" },
  title:          { fontSize: 30, fontWeight: "bold", marginBottom: 6, textAlign: "center", color: "#1a1a1a" },
  subtitle:       { fontSize: 15, color: "#888", textAlign: "center", marginBottom: 28 },
  label:          { fontSize: 14, fontWeight: "600", color: "#333", marginBottom: 6 },
  input:          { borderWidth: 1, borderColor: "#ddd", backgroundColor: "#fff", padding: 13, marginBottom: 16, borderRadius: 10, fontSize: 15 },
  button:         { backgroundColor: "#4CAF50", padding: 15, borderRadius: 10, alignItems: "center", marginTop: 4, marginBottom: 20 },
  buttonDisabled: { opacity: 0.7 },
  btnText:        { color: "#fff", fontWeight: "bold", fontSize: 16 },
  link:           { textAlign: "center", color: "#555", fontSize: 14 },
  linkBold:       { color: "#4CAF50", fontWeight: "bold" },
  errorBox:       { backgroundColor: "#ffe0e0", borderRadius: 8, padding: 12, marginBottom: 16 },
  errorText:      { color: "#c0392b", fontSize: 13, textAlign: "center" },
});
