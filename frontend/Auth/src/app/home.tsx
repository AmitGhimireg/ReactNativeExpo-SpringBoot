import React, { useEffect, useState } from "react";
import { View, Text, TouchableOpacity, StyleSheet, Alert } from "react-native";
import { router } from "expo-router";
import { authApi, clearAuth, getRefreshToken } from "../utils/api";

export default function Home() {
  const [user, setUser] = useState<any>(null);

  useEffect(() => {
    load();
  }, []);

  const load = async () => {
    try {
      const data = await authApi.profile();
      setUser(data);
    } catch {
      await clearAuth();
      router.replace("/login");
    }
  };

  const logout = async () => {
    Alert.alert("Logout", "Are you sure?", [
      { text: "Cancel" },
      {
        text: "Logout",
        style: "destructive",
        onPress: async () => {
          try {
            const token = await getRefreshToken();
            if (token) await authApi.logout(token);
          } catch {}

          await clearAuth();
          router.replace("/login");
        },
      },
    ]);
  };

  return (
    <View style={styles.container}>
      {/* NAVBAR */}
      <View style={styles.navbar}>
        <Text style={styles.navText}>JWT AUTH APP</Text>
      </View>

      {/* BODY */}
      <View style={styles.body}>
        <Text style={styles.title}>Welcome</Text>
        <Text>{user?.fullName}</Text>
        <Text>{user?.email}</Text>

        {!user?.emailVerified ? (
          <Text style={styles.warn}>Verify your email</Text>
        ) : (
          <Text style={styles.ok}>Verified</Text>
        )}
      </View>

      {/* FOOTER */}
      <View style={styles.footer}>
        <TouchableOpacity style={styles.btn} onPress={logout}>
          <Text style={{ color: "#fff" }}>Logout</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#fff" },
  navbar: {
    height: 60,
    backgroundColor: "#1e90ff",
    alignItems: "center",
    justifyContent: "center",
  },
  navText: { color: "#fff", fontSize: 18, fontWeight: "bold" },
  body: { flex: 1, padding: 20, alignItems: "center", justifyContent: "center" },
  title: { fontSize: 24, marginBottom: 10 },
  warn: { color: "#ff8c00", marginTop: 10 },
  ok: { color: "#2e8b57", marginTop: 10 },
  footer: { padding: 20, borderTopWidth: 1, borderColor: "#eee" },
  btn: {
    backgroundColor: "#ff4d4f",
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 6,
    alignItems: "center",
  },
});