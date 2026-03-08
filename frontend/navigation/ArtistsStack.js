import { createNativeStackNavigator } from "@react-navigation/native-stack";
import PlaylistDetailScreen from "../screens/PlaylistDetailScreen";
import HeaderMenuButton from "../components/HeaderMenuButton";
import ArtistsScreen from "../screens/ArtistsScreen";

const Stack = createNativeStackNavigator();

const ArtistsStack = () => {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: "#333" },
        headerTintColor: "#fff",
      }}
    >
      <Stack.Screen
        name="ArtistsScreen"
        component={ArtistsScreen}
        options={{
          title: "Artistas",
          headerLeft: () => <HeaderMenuButton />,
        }}
      />
      <Stack.Screen
        name="PlaylistDetail"
        component={PlaylistDetailScreen}
        options={{ title: "Artista" }}
      />
    </Stack.Navigator>
  );
};

export default ArtistsStack;
