import { useState, useCallback, useEffect } from "react";
import {
  FlatList,
  TextInput,
  View,
  StyleSheet,
  ActivityIndicator,
  Text,
  TouchableOpacity,
} from "react-native";
import PlaylistCard from "./PlaylistCard";
import Colors from "../services/colors";
import { Ionicons } from "@expo/vector-icons";
import {
  subscribeToPlaylistsModifiedEvent,
  unsubscribeFromPlaylistsModifiedEvent,
} from "../events/playlistsModifiedEvent";
import {
  subscribeToFavPlaylistsModifiedEvent,
  unsubscribeFromFavPlaylistsModifiedEvent,
} from "../events/favPlaylistsModifiedEvent";
import { useIsFocused } from "@react-navigation/native";

const PlaylistList = ({ fetchFunction, extraHeader }) => {
  const [playlists, setPlaylists] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [search, setSearch] = useState("");
  const [offset, setOffset] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const limit = __DEV__ ? 5 : 20;

  const isFocused = useIsFocused();
  const [isDirty, setIsDirty] = useState(false);

  const loadData = async (reset = false, searchOverride = null) => {
    if (reset) {
      setLoading(true);
      setOffset(0);
    }

    try {
      const currentOffset = reset ? 0 : offset;

      const searchTerm = searchOverride !== null ? searchOverride : search;
      const data = await fetchFunction(currentOffset, limit, searchTerm);

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

  useEffect(() => {
    loadData(true);
  }, []);

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

  useEffect(() => {
    const listener = subscribeToFavPlaylistsModifiedEvent(() => {
      if (!isFocused) {
        loadData(true);
      } else {
        setIsDirty(true);
      }
    });

    return () => unsubscribeFromFavPlaylistsModifiedEvent(listener);
  }, [isFocused]);

  useEffect(() => {
    if (!isFocused && isDirty) {
      loadData(true);
      setIsDirty(false);
    }
  }, [isFocused, isDirty]);

  const handleClearSearch = () => {
    setSearch("");
    loadData(true, "");
  };

  return (
    <View style={styles.container}>
      <View style={styles.fixedHeader}>
        {extraHeader}
        <View style={styles.searchContainer}>
          <TouchableOpacity
            onPress={() => loadData(true)}
            style={styles.searchIconButton}
          >
            <Ionicons name="search" size={20} color="#888" />
          </TouchableOpacity>

          <TextInput
            style={styles.searchInput}
            placeholder="Buscar playlist..."
            placeholderTextColor="#888"
            value={search}
            onChangeText={setSearch}
            onSubmitEditing={() => loadData(true)}
            returnKeyType="search"
          />

          {/* BOTÓN CLEAR: Solo se muestra si hay texto */}
          {search.length > 0 && (
            <TouchableOpacity
              onPress={handleClearSearch}
              style={styles.clearButton}
            >
              <Ionicons name="close-circle" size={20} color="#888" />
            </TouchableOpacity>
          )}
        </View>
      </View>

      {/* SECCIÓN DINÁMICA: Spinner o Lista */}
      {loading && offset === 0 ? (
        <View style={styles.centerLoader}>
          <ActivityIndicator size="large" color={Colors.PRIMARY_COLOR} />
        </View>
      ) : (
        <FlatList
          data={playlists}
          keyExtractor={(item) => item.id.toString()}
          renderItem={({ item }) => <PlaylistCard playlist={item} />}
          onEndReached={handleLoadMore}
          onEndReachedThreshold={0.3}
          refreshing={refreshing}
          onRefresh={handleRefresh}
          ListFooterComponent={
            loadingMore ? (
              <ActivityIndicator color={Colors.PRIMARY_COLOR} />
            ) : null
          }
          ListEmptyComponent={
            <Text style={styles.emptyText}>No se encontraron playlists</Text>
          }
          contentContainerStyle={styles.listContent}
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#121212", // Asegura que el fondo sea uniforme
  },
  fixedHeader: {
    paddingHorizontal: 16,
    paddingTop: 10,
    backgroundColor: "#121212",
  },
  centerLoader: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
  },
  listContent: {
    paddingHorizontal: 16,
    paddingBottom: 100,
  },
  searchContainer: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "#222",
    borderRadius: 10,
    paddingHorizontal: 12, // Volvemos a tus 12 de padding
    marginBottom: 20, // Mantenemos tu margen original
  },
  searchInput: {
    color: "#fff",
    height: 45,
    flex: 1,
  },
  searchIconButton: {
    padding: 10,
    marginLeft: -10, // Ajuste para compensar el padding del container
  },
  emptyText: {
    color: "#888",
    textAlign: "center",
    marginTop: 20,
    fontStyle: "italic",
  },
});

export default PlaylistList;
