import { View, StyleSheet } from "react-native";
import PlaylistList from "../components/PlaylistList";
import { fetchUserFavoritePlaylists } from "../services/apiService";
import NewPlaylistButton from "../components/NewPlaylistButton";
import { useNavigation } from "@react-navigation/native";
import { useCallback, useEffect, useState } from "react";

const FavoritePlaylistsScreen = () => {
  const navigation = useNavigation();
  const [currentUserId, setCurrentUserId] = useState(null);

  useEffect(() => {
    const loadUserId = async () => {
      const id = await getLoggedUserId();
      setCurrentUserId(id);
    };
    loadUserId();
  }, []);

  const handleFetchFavorites = useCallback(
    async (offset, limit, search) => {
      return await fetchUserFavoritePlaylists(
        currentUserId,
        offset,
        limit,
        search,
      );
    },
    [currentUserId], // Se recrea solo si el ID cambia
  );

  return (
    <View style={styles.container}>
      <PlaylistList fetchFunction={handleFetchFavorites} />

      <NewPlaylistButton
        onPress={() => navigation.navigate("NewPlaylistScreen")}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#121212" },
});

export default FavoritePlaylistsScreen;
