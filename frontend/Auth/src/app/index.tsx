import { Redirect } from "expo-router";
import { useEffect, useState } from "react";
import { ActivityIndicator, View } from "react-native";
import { getAccessToken, getUser } from "../utils/api";

/**
 * index.tsx — App entry point. Decides where to redirect on launch.
 *
 * LOGIC:
 *   • Checks AsyncStorage for a stored access token.
 *   • If found → redirect to /admin or /home, based on the stored role.
 *   • If not found → redirect to /login.
 *
 * Note: The destination screen also calls /api/auth/profile which will
 * trigger a token refresh if the access token has expired, or redirect to
 * login if the refresh token is also expired. It also re-verifies the role
 * itself, so a stale/edited local "role" value can't grant extra access.
 */
export default function Index() {
  const [loading, setLoading]   = useState(true);
  const [destination, setDestination] = useState<"/login" | "/home" | "/admin">("/login");

  useEffect(() => {
    // Check for an existing access token + cached user in AsyncStorage
    (async () => {
      const token = await getAccessToken();
      if (!token) {
        setDestination("/login");
      } else {
        const user = await getUser();
        setDestination(user?.role === "ADMIN" ? "/admin" : "/home");
      }
      setLoading(false);
    })();
  }, []);

  if (loading) {
    return (
      <View style={{ flex: 1, justifyContent: "center", alignItems: "center" }}>
        <ActivityIndicator size="large" color="#4CAF50" />
      </View>
    );
  }

  // Route based on token presence + cached role
  return <Redirect href={destination} />;
}
