// navigation/UserPlaylistsStack.js

import React from "react";
import AddSongsScreen from "../screens/AddSongsScreen";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import UserPlaylistsScreen from "../screens/UserPlaylistsScreen";
import NewPlaylistScreen from "../screens/NewPlaylistScreen";
import { SafeAreaView } from "react-native-safe-area-context";
import HeaderMenuButton from "../components/HeaderMenuButton";
import PlaylistDetailScreen from "../screens/PlaylistDetailScreen";
import SugerenciasScreen from "../screens/SugerenciasScreen";

const Stack = createNativeStackNavigator();

const UserPlaylistsStack = () => {
  return (
    <SafeAreaView style={{ flex: 1 }}>
      <Stack.Navigator
        screenOptions={{
          headerStyle: { backgroundColor: "#333" },
          headerTintColor: "#fff",
        }}
      >
        <Stack.Screen
          name="UserPlaylists"
          component={UserPlaylistsScreen}
          options={{
            title: "Mis playlists",
            headerLeft: () => <HeaderMenuButton />,
          }}
        />
        <Stack.Screen
          name="NewPlaylistScreen"
          component={NewPlaylistScreen}
          options={{ title: "Nueva Playlist" }}
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

export default UserPlaylistsStack;
