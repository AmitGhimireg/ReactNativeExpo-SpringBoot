import React, { useEffect, useState } from "react";
import { View, Text, ActivityIndicator, StyleSheet } from "react-native";
import { useLocalSearchParams, useRouter } from "expo-router";
import api from "@/services/authService";

const API_URL = "http://192.168.100.34:8080/api/auth";

export default function VerifyEmailScreen() {
  const { token } = useLocalSearchParams<{ token?: string }>();
  const router = useRouter();

  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    const verifyEmail = async () => {
      if (!token) {
        setMessage(
          "Verification token is missing. Please use the verification link sent to your email.",
        );
        setSuccess(false);
        setLoading(false);
        return;
      }

      try {
        const res = await api({
          url: `${API_URL}/verify-email?token=${token}`,
          method: "GET",
        });

        setMessage(res.data.message || "Email verified successfully!");

        setSuccess(true);

        setTimeout(() => {
          router.replace("/login");
        }, 3000);
      } catch (error: any) {
        setMessage(error?.response?.data?.message || "Verification failed.");

        setSuccess(false);
      } finally {
        setLoading(false);
      }
    };

    verifyEmail();
  }, [token]);

  return (
    <View style={styles.container}>
      {loading ? (
        <>
          <ActivityIndicator size="large" />
          <Text style={styles.info}>Verifying your email...</Text>
        </>
      ) : (
        <>
          <Text
            style={[styles.message, success ? styles.success : styles.error]}
          >
            {message}
          </Text>

          {success && (
            <Text style={styles.redirect}>Redirecting to login...</Text>
          )}
        </>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    padding: 24,
    backgroundColor: "#fff",
  },
  info: {
    marginTop: 15,
    fontSize: 16,
  },
  message: {
    textAlign: "center",
    fontSize: 16,
    fontWeight: "600",
  },
  success: {
    color: "green",
  },
  error: {
    color: "red",
  },
  redirect: {
    marginTop: 15,
    color: "#666",
  },
});
