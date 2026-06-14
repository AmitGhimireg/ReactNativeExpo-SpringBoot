import { Stack } from "expo-router";

/**
 * _layout.tsx — Root navigator for the Expo Router app.
 * Defines all screens in the stack. headerShown: false gives us full
 * control over the UI in each screen without the default nav bar.
 */
export default function Layout() {
  return (
    <Stack>
      <Stack.Screen name="index" options={{ headerShown: false }} />
      <Stack.Screen name="login" options={{ headerShown: false }} />
      <Stack.Screen name="register" options={{ headerShown: false }} />
      <Stack.Screen name="home" options={{ headerShown: false }} />
      <Stack.Screen name="verify-email" options={{ headerShown: false }} />
    </Stack>
  );
}
