import { View, StyleSheet } from "react-native";
import PlaylistList from "../components/PlaylistList";
import { fetchUserFavoritePlaylists } from "../services/apiService";
import NewPlaylistButton from "../components/NewPlaylistButton";
import { useNavigation } from "@react-navigation/native";
import { useCallback, useEffect, useState } from "react";
import {
  emitFavPlaylistsModifiedEvent,
  subscribeToFavPlaylistsModifiedEvent,
  unsubscribeFromFavPlaylistsModifiedEvent,
} from "../events/favPlaylistsModifiedEvent";

const FavoritePlaylistsScreen = () => {
  const navigation = useNavigation();
  const [currentUserId, setCurrentUserId] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);

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

  useEffect(() => {
    const listener = subscribeToFavPlaylistsModifiedEvent(() => {
      // volver a montar playlist list para refrescar datos
      setRefreshKey((prev) => prev + 1);
    });

    // Limpiar el listener cuando el componente se desmonte
    return () => {
      unsubscribeFromFavPlaylistsModifiedEvent(listener);
    };
  }, []);

  return (
    <View style={styles.container}>
      <PlaylistList
        key={refreshKey}
        fetchFunction={handleFetchFavorites}
        publicCard={true}
        emitFavEvent={false}
      />

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
