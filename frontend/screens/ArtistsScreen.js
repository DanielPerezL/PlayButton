import { View, StyleSheet } from "react-native";
import PlaylistList from "../components/PlaylistList";
import { getAllArtists } from "../services/apiService";

const ArtistsScreen = () => {
  return (
    <View style={styles.container}>
      <PlaylistList fetchFunction={getAllArtists} isArtists={true} />
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#121212" },
});

export default ArtistsScreen;
