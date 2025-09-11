import React from "react";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import PublicPlaylistsScreen from "../screens/PublicPlaylistsScreen";
import PlaylistDetailScreen from "../screens/PlaylistDetailScreen";
import HeaderMenuButton from "../components/HeaderMenuButton";
import { SafeAreaView } from "react-native-safe-area-context";
import AddSongsScreen from "../screens/AddSongsScreen";
import SugerenciasScreen from "../screens/SugerenciasScreen";

const Stack = createNativeStackNavigator();

const PublicPlaylistsStack = () => {
  return (
    <SafeAreaView style={{ flex: 1 }}>
      <Stack.Navigator
        screenOptions={{
          headerStyle: { backgroundColor: "#333" },
          headerTintColor: "#fff",
        }}
      >
        <Stack.Screen
          name="PublicPlaylists"
          component={PublicPlaylistsScreen}
          options={{
            title: "Playlist públicas",
            headerLeft: () => <HeaderMenuButton />,
          }}
        />
        <Stack.Screen
          name="PlaylistDetail"
          component={PlaylistDetailScreen}
          options={{ title: "Detalles de Playlist" }}
        />
        <Stack.Screen
          name="AddSongsScreen"
          component={AddSongsScreen}
          options={{ title: "Añadir canciones" }}
        />
        <Stack.Screen
          name="SugerenciasScreen"
          component={SugerenciasScreen}
          options={{ title: "Sugerir canciones" }}
        />
      </Stack.Navigator>
    </SafeAreaView>
  );
};

export default PublicPlaylistsStack;
