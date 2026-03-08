import { createNativeStackNavigator } from "@react-navigation/native-stack";
import PlaylistDetailScreen from "../screens/PlaylistDetailScreen";
import HeaderMenuButton from "../components/HeaderMenuButton";
import AddSongsScreen from "../screens/AddSongsScreen";
import SugerenciasScreen from "../screens/SugerenciasScreen";
import NewPlaylistScreen from "../screens/NewPlaylistScreen";
import UserPlaylistsScreen from "../screens/UserPlaylistsScreen";
import FavoritePlaylistsScreen from "../screens/FavoritePlaylistsScreen";

const Stack = createNativeStackNavigator();

const UserFavsPlaylistStack = () => {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: "#333" },
        headerTintColor: "#fff",
      }}
    >
      <Stack.Screen
        name="PublicPlaylists"
        component={FavoritePlaylistsScreen}
        options={{
          title: "Mis playlist favoritas",
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
        name="UserPlaylists"
        component={UserPlaylistsScreen}
        options={{ title: "Playlist públicas" }}
      />
      <Stack.Screen
        name="NewPlaylistScreen"
        component={NewPlaylistScreen}
        options={{ title: "Nueva Playlist" }}
      />
      <Stack.Screen
        name="SugerenciasScreen"
        component={SugerenciasScreen}
        options={{ title: "Sugerir canciones" }}
      />
    </Stack.Navigator>
  );
};

export default UserFavsPlaylistStack;
