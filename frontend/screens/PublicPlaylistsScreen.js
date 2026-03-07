import { View, StyleSheet, TouchableOpacity } from "react-native";
import PlaylistList from "../components/PlaylistList";
import { getAllPlaylists } from "../services/apiService";
import NewPlaylistButton from "../components/NewPlaylistButton";
import { useNavigation } from "@react-navigation/native";

const PublicPlaylistsScreen = () => {
  const navigation = useNavigation();

  return (
    <View style={styles.container}>
      <PlaylistList fetchFunction={getAllPlaylists} publicCard={true} />

      <NewPlaylistButton
        onPress={() => navigation.navigate("NewPlaylistScreen")}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#121212" },
});

export default PublicPlaylistsScreen;
