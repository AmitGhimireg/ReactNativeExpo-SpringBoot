import React, { useState } from "react";
import {
  View, Text, TextInput, TouchableOpacity,
  StyleSheet, ActivityIndicator, KeyboardAvoidingView,
  Platform, ScrollView,
} from "react-native";
import { router } from "expo-router";
import { authApi, saveAccessToken, saveRefreshToken, saveUser } from "../utils/api";

/**
 * register.tsx — Registration screen.
 *
 * FLOW:
 *   1. User fills in full name, email, password, confirm password.
 *   2. Client-side validation runs first.
 *   3. Calls POST /api/auth/register.
 *   4. Backend saves user (emailVerified=false) and sends verification email.
 *   5. Frontend receives accessToken + refreshToken → saves both → navigates to /home.
 *   6. Home screen shows a "please verify your email" banner if emailVerified=false.
 *
 * MODIFIED: Now saves refreshToken so the user stays logged in after registration.
 */
export default function Register() {
  const [fullName, setFullName] = useState("");
  const [email, setEmail]       = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm]   = useState("");
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState("");
  const [success, setSuccess]   = useState("");

  const handleRegister = async () => {
    setError("");
    setSuccess("");

    // Client-side validation
    if (!fullName.trim() || !email.trim() || !password.trim()) {
      setError("All fields are required.");  return;
    }
    if (fullName.trim().length < 2) {
      setError("Full name must be at least 2 characters.");  return;
    }
    if (!email.includes("@")) {
      setError("Please enter a valid email address.");  return;
    }
    if (password.length < 6) {
      setError("Password must be at least 6 characters.");  return;
    }
    if (password !== confirm) {
      setError("Passwords do not match.");  return;
    }

    setLoading(true);
    try {
      const data = await authApi.register(
        fullName.trim(),
        email.trim().toLowerCase(),
        password
      );

      // Save both tokens — user is auto-logged in after registration
      await saveAccessToken(data.accessToken);
      await saveRefreshToken(data.refreshToken);  // ADDED: refresh token for session renewal
      await saveUser({
        email:         data.email,
        fullName:      data.fullName,
        emailVerified: data.emailVerified,   // ADDED: false at this point
      });

      setSuccess("Account created! Please check your email to verify your account.");
      setTimeout(() => router.replace("/home"), 1200);
    } catch (err: any) {
      setError(err.message || "Registration failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === "ios" ? "padding" : undefined}>
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <Text style={styles.title}>Create Account</Text>
        <Text style={styles.subtitle}>Join us today</Text>

        {!!error   && <View style={styles.errorBox}><Text style={styles.errorText}>{error}</Text></View>}
        {!!success && <View style={styles.successBox}><Text style={styles.successText}>{success}</Text></View>}

        <Text style={styles.label}>Full Name</Text>
        <TextInput placeholder="Enter your full name" style={styles.input} value={fullName} onChangeText={setFullName} autoComplete="name" />

        <Text style={styles.label}>Email</Text>
        <TextInput placeholder="Enter your email" style={styles.input} value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" autoComplete="email" />

        <Text style={styles.label}>Password</Text>
        <TextInput placeholder="Min. 6 characters" secureTextEntry style={styles.input} value={password} onChangeText={setPassword} autoComplete="new-password" />

        <Text style={styles.label}>Confirm Password</Text>
        <TextInput placeholder="Re-enter your password" secureTextEntry style={styles.input} value={confirm} onChangeText={setConfirm} />

        <TouchableOpacity style={[styles.button, loading && styles.buttonDisabled]} onPress={handleRegister} disabled={loading}>
          {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.btnText}>Create Account</Text>}
        </TouchableOpacity>

        <TouchableOpacity onPress={() => router.push("/login")}>
          <Text style={styles.link}>Already have an account? <Text style={styles.linkBold}>Login</Text></Text>
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
  button:         { backgroundColor: "#2196F3", padding: 15, borderRadius: 10, alignItems: "center", marginTop: 4, marginBottom: 20 },
  buttonDisabled: { opacity: 0.7 },
  btnText:        { color: "#fff", fontWeight: "bold", fontSize: 16 },
  link:           { textAlign: "center", color: "#555", fontSize: 14 },
  linkBold:       { color: "#2196F3", fontWeight: "bold" },
  errorBox:       { backgroundColor: "#ffe0e0", borderRadius: 8, padding: 12, marginBottom: 16 },
  errorText:      { color: "#c0392b", fontSize: 13, textAlign: "center" },
  successBox:     { backgroundColor: "#e0f7e9", borderRadius: 8, padding: 12, marginBottom: 16 },
  successText:    { color: "#27ae60", fontSize: 13, textAlign: "center" },
});
