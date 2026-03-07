import { useState, useCallback, useEffect } from "react";
import {
  FlatList,
  TextInput,
  View,
  StyleSheet,
  ActivityIndicator,
  Text,
} from "react-native";
import PlaylistCard from "./PlaylistCard";
import Colors from "../services/colors";
import { Ionicons } from "@expo/vector-icons";
import {
  subscribeToPlaylistsModifiedEvent,
  unsubscribeFromPlaylistsModifiedEvent,
} from "../events/playlistsModifiedEvent";

const PlaylistList = ({
  fetchFunction,
  publicCard,
  extraHeader,
  emitFavEvent = true,
}) => {
  const [playlists, setPlaylists] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [search, setSearch] = useState("");
  const [offset, setOffset] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const limit = __DEV__ ? 5 : 20;

  const loadData = async (reset = false) => {
    if (reset) {
      setLoading(true);
      setOffset(0);
    }

    try {
      const currentOffset = reset ? 0 : offset;
      // La fetchFunction debe aceptar (offset, limit, search)
      const data = await fetchFunction(currentOffset, limit, search);

      const newList = data.playlists || [];
      setPlaylists((prev) => (reset ? newList : [...prev, ...newList]));
      setHasMore(data.has_more);
      setOffset((prev) => currentOffset + newList.length);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
      setLoadingMore(false);
      setRefreshing(false);
    }
  };

  // Debounce para la búsqueda
  useEffect(() => {
    const delayDebounceFn = setTimeout(() => {
      loadData(true);
    }, 500);
    return () => clearTimeout(delayDebounceFn);
  }, [search]);

  const handleRefresh = () => {
    setRefreshing(true);
    loadData(true);
  };

  const handleLoadMore = () => {
    if (hasMore && !loadingMore && !loading) {
      setLoadingMore(true);
      loadData(false);
    }
  };

  useEffect(() => {
    const listener = subscribeToPlaylistsModifiedEvent(() => {
      loadData(true); // Actualizar las playlists cuando se crea una nueva
    });

    // Limpiar el listener cuando el componente se desmonte
    return () => {
      unsubscribeFromPlaylistsModifiedEvent(listener);
    };
  }, []);

  const renderHeader = () => (
    <View>
      {extraHeader}
      <View style={styles.searchContainer}>
        <Ionicons
          name="search"
          size={20}
          color="#888"
          style={styles.searchIcon}
        />
        <TextInput
          style={styles.searchInput}
          placeholder="Buscar playlist..."
          placeholderTextColor="#888"
          value={search}
          onChangeText={setSearch}
        />
      </View>
    </View>
  );

  if (loading && offset === 0) {
    return (
      <ActivityIndicator
        size="large"
        color={Colors.PRIMARY_COLOR}
        style={{ marginTop: 50 }}
      />
    );
  }

  return (
    <FlatList
      data={playlists}
      keyExtractor={(item) => item.id.toString()}
      renderItem={({ item }) => (
        <PlaylistCard
          playlist={item}
          publicCard={publicCard}
          emitFavEvent={emitFavEvent}
        />
      )}
      ListHeaderComponent={renderHeader}
      onEndReached={handleLoadMore}
      onEndReachedThreshold={0.3} // Carga cuando falte el 30% para llegar al final
      refreshing={refreshing}
      onRefresh={handleRefresh}
      ListFooterComponent={
        loadingMore ? <ActivityIndicator color={Colors.PRIMARY_COLOR} /> : null
      }
      ListEmptyComponent={
        <Text style={styles.emptyText}>No se encontraron playlists</Text>
      }
      contentContainerStyle={styles.listContent}
    />
  );
};

const styles = StyleSheet.create({
  listContent: { padding: 16, paddingBottom: 100 },
  searchContainer: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "#222",
    borderRadius: 10,
    paddingHorizontal: 12,
    marginBottom: 20,
  },
  searchIcon: { marginRight: 8 },
  searchInput: { color: "#fff", height: 45, flex: 1 },
  emptyText: {
    color: "#888",
    textAlign: "center",
    marginTop: 20,
    fontStyle: "italic",
  },
});

export default PlaylistList;
