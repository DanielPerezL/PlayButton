import { useState, useEffect } from "react";
import { clearPlaylistCache, isLoggedIn, logout } from "./services/apiService";
import { StatusBar } from "expo-status-bar";
import TrackPlayer from "react-native-track-player";
import { NavigationContainer } from "@react-navigation/native";
import { createDrawerNavigator } from "@react-navigation/drawer";
import AuthWrapper from "./components/AuthWrapper";
import authEventEmitter from "./events/authEvent";
import CustomDrawerContent from "./components/CustomDrawerContent";
import UserPlaylistsStack from "./navigation/UserPlaylistStack";
import { SafeAreaProvider } from "react-native-safe-area-context";
import HeaderMenuButton from "./components/HeaderMenuButton";
import PublicPlaylistsStack from "./navigation/PublicPlaylistsStack";
import Colors from "./services/colors";
import { AlertProvider } from "./services/alertContext";
import ConfigurationStack from "./navigation/ConfigurationStack";
import PlayerScreen from "./screens/PlayerScreen";

const App = () => {
  const [logged, setLogged] = useState(false);
  const Drawer = createDrawerNavigator();

  useEffect(() => {
    const checkLogin = async () => {
      const stored = await isLoggedIn();
      setLogged(stored);
    };
    checkLogin();
  }, []);

  useEffect(() => {
    clearPlaylistCache();
  }, []);

  const handleLogout = async () => {
    await TrackPlayer.stop();
    await TrackPlayer.reset();
    await logout();
    setLogged(false);
  };

  useEffect(() => {
    const handleAuthChange = (status) => {
      setLogged(status);
    };

    // Suscribirse al evento
    const subscription = authEventEmitter.addListener(
      "authStatusChanged",
      handleAuthChange
    );

    return () => {
      subscription.remove();
    };
  }, []);

  useEffect(() => {
    if (!logged) handleLogout();
  }, [logged]);

  return (
    <AlertProvider>
      <SafeAreaProvider>
        <StatusBar style="light" backgroundColor="#222" />
        <AuthWrapper logged={logged} onLoginSuccess={() => setLogged(true)}>
          <NavigationContainer>
            <Drawer.Navigator
              drawerContent={(drawerProps) => (
                <CustomDrawerContent {...drawerProps} onLogout={handleLogout} />
              )}
              screenOptions={{
                drawerActiveTintColor: Colors.DRAWER_ACTIVE_TINT_COLOR,
                headerLeft: () => (
                  <HeaderMenuButton
                    style={{ marginLeft: 15, marginRight: 7 }}
                  />
                ),
                headerStyle: {
                  backgroundColor: "#333",
                },
                headerTintColor: "#fff",
                drawerStyle: {
                  backgroundColor: "#222",
                },
                drawerLabelStyle: {
                  color: "#fff",
                  fontWeight: "bold",
                },
              }}
            >
              <Drawer.Screen name="Reproductor">
                {() => <PlayerScreen />}
              </Drawer.Screen>
              <Drawer.Screen
                name="Mis playlists"
                component={UserPlaylistsStack}
                options={{ headerShown: false }}
              />
              <Drawer.Screen
                name="Playlist públicas"
                component={PublicPlaylistsStack}
                options={{ headerShown: false }}
              />
              <Drawer.Screen
                name="Configuración"
                component={ConfigurationStack}
                options={{
                  headerShown: false,
                  drawerItemStyle: { display: "none" },
                }}
              />
            </Drawer.Navigator>
          </NavigationContainer>
        </AuthWrapper>
      </SafeAreaProvider>
    </AlertProvider>
  );
};

export default App;
