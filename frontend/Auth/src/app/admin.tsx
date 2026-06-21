import React, { useEffect, useState } from "react";
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Alert,
  FlatList,
  ActivityIndicator,
} from "react-native";
import { router } from "expo-router";
import { authApi, clearAuth, getRefreshToken } from "../utils/api";

/**
 * admin.tsx — Admin-only dashboard.
 *
 * GUARD: on mount it calls /api/auth/profile (always trustworthy — it's
 * read from the DB on the backend). If the logged-in user is NOT an Admin,
 * they're redirected to /home instead of seeing this screen.
 *
 * It also calls the Admin-only backend endpoint /api/admin/users to prove
 * the role check on the SERVER (SecurityConfig) is actually working — a
 * Client token hitting that endpoint would get a 403, not just be hidden
 * by the UI.
 */
export default function Admin() {
  const [user, setUser] = useState<any>(null);
  const [users, setUsers] = useState<any[]>([]);
  const [loadingUsers, setLoadingUsers] = useState(true);
  const [checking, setChecking] = useState(true);

  useEffect(() => {
    load();
  }, []);

  const load = async () => {
    try {
      const profile = await authApi.profile();

      if (profile.role !== "ADMIN") {
        router.replace("/home");
        return;
      }
      setUser(profile);
      setChecking(false);

      const allUsers = await authApi.adminUsers();
      setUsers(allUsers);
    } catch {
      await clearAuth();
      router.replace("/login");
    } finally {
      setLoadingUsers(false);
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

  if (checking) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#222" />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {/* NAVBAR */}
      <View style={styles.navbar}>
        <Text style={styles.navText}>ADMIN DASHBOARD</Text>
      </View>

      {/* BODY */}
      <View style={styles.body}>
        <Text style={styles.title}>Welcome, {user?.fullName}</Text>
        <Text style={styles.roleTag}>Role: {user?.role}</Text>

        <Text style={styles.sectionTitle}>All Users</Text>

        {loadingUsers ? (
          <ActivityIndicator />
        ) : (
          <FlatList
            data={users}
            keyExtractor={(item) => String(item.id)}
            style={styles.list}
            renderItem={({ item }) => (
              <View style={styles.userRow}>
                <View>
                  <Text style={styles.userName}>{item.fullName}</Text>
                  <Text style={styles.userEmail}>{item.email}</Text>
                </View>
                <Text
                  style={[
                    styles.userRole,
                    item.role === "ADMIN" && styles.userRoleAdmin,
                  ]}
                >
                  {item.role}
                </Text>
              </View>
            )}
          />
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
  center: { flex: 1, justifyContent: "center", alignItems: "center" },
  container: { flex: 1, backgroundColor: "#fff" },
  navbar: {
    height: 60,
    backgroundColor: "#222",
    alignItems: "center",
    justifyContent: "center",
  },
  navText: { color: "#fff", fontSize: 18, fontWeight: "bold" },
  body: { flex: 1, padding: 20 },
  title: { fontSize: 22, marginBottom: 2 },
  roleTag: { color: "#888", marginBottom: 20 },
  sectionTitle: { fontSize: 16, fontWeight: "bold", marginBottom: 8 },
  list: { flex: 1 },
  userRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderColor: "#eee",
  },
  userName: { fontSize: 14, fontWeight: "600" },
  userEmail: { fontSize: 12, color: "#888" },
  userRole: {
    fontSize: 12,
    fontWeight: "bold",
    color: "#2e8b57",
    backgroundColor: "#eafaf1",
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  userRoleAdmin: { color: "#1e90ff", backgroundColor: "#e8f3ff" },
  footer: { padding: 20, borderTopWidth: 1, borderColor: "#eee" },
  btn: {
    backgroundColor: "#ff4d4f",
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 6,
    alignItems: "center",
  },
});
