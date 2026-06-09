import { Redirect } from "expo-router";
import { useEffect, useState } from "react";
import { ActivityIndicator, View } from "react-native";
import { getAccessToken } from "../utils/api";

/**
 * index.tsx — App entry point. Decides where to redirect on launch.
 *
 * LOGIC:
 *   • Checks AsyncStorage for a stored access token.
 *   • If found → redirect to /home (user is likely still logged in).
 *   • If not found → redirect to /login.
 *
 * Note: The home screen also calls /api/auth/profile which will trigger a
 * token refresh if the access token has expired, or redirect to login if
 * the refresh token is also expired.
 */
export default function Index() {
  const [loading, setLoading]   = useState(true);
  const [hasToken, setHasToken] = useState(false);

  useEffect(() => {
    // Check for an existing access token in AsyncStorage
    getAccessToken().then((token) => {
      setHasToken(!!token);
      setLoading(false);
    });
  }, []);

  if (loading) {
    return (
      <View style={{ flex: 1, justifyContent: "center", alignItems: "center" }}>
        <ActivityIndicator size="large" color="#4CAF50" />
      </View>
    );
  }

  // Route based on token presence
  return <Redirect href={hasToken ? "/home" : "/login"} />;
}
