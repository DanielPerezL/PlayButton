import { useEffect, useState } from "react";
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  ActivityIndicator,
} from "react-native";
import { useRoute } from "@react-navigation/native";
import {
  searchSongsByName,
  addSongToPlaylist,
  clearPlaylistIdCache,
  getCachedSongs,
  getHideAddSongAlert,
  setHideAddSongAlert,
} from "../services/apiService";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import Colors from "../services/colors";
import { useAlert } from "../services/alertContext";

const AddSongsScreen = () => {
  const { showAlert } = useAlert();

  const navigation = useNavigation();
  const route = useRoute();
  const { playlistId } = route.params;
  const [playlistSongs, setPlaylistSongs] = useState([]);

  const [searchTerm, setSearchTerm] = useState("");
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(false);
  const [offset, setOffset] = useState(0);
  const [isSearching, setIsSearching] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [hasAddedSongs, setHasAddedSongs] = useState(false);
  const [addingSong, setAddingSong] = useState(false); // Estado para controlar el indicador de carga

  useEffect(() => {
    const getSongs = async () => {
      const cached = await getCachedSongs(playlistId);
      if (!cached) return;

      const ids = cached.map((song) => song.id);
      setPlaylistSongs(ids);
    };
    getSongs();
  }, [playlistId]);

  const handleSearch = async () => {
    if (!searchTerm) {
      return;
    }
    setLoading(true);
    setIsSearching(true);
    setHasSearched(true);
    const { songs, hasMore: moreSongs } = await searchSongsByName(
      searchTerm,
      0
    );

    setResults(songs);
    setHasMore(moreSongs);
    setOffset(songs.length);
    setLoading(false);
    setIsSearching(false);
  };

  const handleLoadMore = async () => {
    if (loading || !hasMore) return;
    setLoading(true);

    const { songs, hasMore: moreSongs } = await searchSongsByName(
      searchTerm,
      offset
    );

    setResults((prevResults) => [...prevResults, ...songs]);
    setHasMore(moreSongs);
    setOffset((prevOffset) => prevOffset + songs.length);
    setLoading(false);
  };

  const handleAddSong = async (songId) => {
    try {
      setAddingSong(songId); // Activar el indicador de carga

      await addSongToPlaylist(playlistId, songId);
      await clearPlaylistIdCache(playlistId);
      const hideAlert = await getHideAddSongAlert();
      if (!hideAlert) {
        showAlert("Canción añadida", "La canción ha sido añadida con éxito", [
          {
            text: "No volver a mostrar",
            style: "default",
            onPress: async () => {
              setHideAddSongAlert(true);
            },
          },
          {
            text: "Cerrar",
            style: "cancel",
          },
        ]);
      }
      setHasAddedSongs(true);
      setPlaylistSongs((prev) => [...prev, songId]);
    } catch (err) {
      console.error(err);
      showAlert("Error al añadir canción", err.message);
    } finally {
      setAddingSong(false); // Desactivar el indicador de carga
    }
  };

  useEffect(() => {
    return () => {
      if (hasAddedSongs) {
        navigation.navigate("PlaylistDetail", {
          ...route.params,
          refresh: true,
        });
      }
    };
  }, [hasAddedSongs]);

  useEffect(() => {
    handleSearch();
  }, []);

  const filteredResults = results.filter(
    (item) => !playlistSongs.includes(item.id)
  );

  const renderItem = ({ item }) => {
    const color = Colors.PRIMARY_PASTEL_COLOR;

    return (
      <View style={styles.songItem}>
        <Text style={styles.songText}>{item.name || `Canción ${item.id}`}</Text>
        <TouchableOpacity onPress={() => handleAddSong(item.id)}>
          {addingSong === item.id ? (
            <ActivityIndicator size={28} color={color} />
          ) : (
            <Ionicons name="add-circle" size={28} color={color} />
          )}
        </TouchableOpacity>
      </View>
    );
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Buscar canciones</Text>
      <View style={styles.searchContainer}>
        <TextInput
          style={styles.input}
          placeholder="Nombre de la canción"
          value={searchTerm}
          onChangeText={(text) => {
            setSearchTerm(text);
            setHasSearched(false);
          }}
          placeholderTextColor="#888"
          onSubmitEditing={handleSearch}
        />
        <TouchableOpacity onPress={handleSearch} style={styles.searchButton}>
          <Ionicons name="search" size={24} color="#fff" />
        </TouchableOpacity>
      </View>
      <FlatList
        data={filteredResults}
        keyExtractor={(item) => item.id.toString()}
        renderItem={renderItem}
        ListEmptyComponent={() => {
          if (!hasSearched) {
            return (
              <Text style={styles.startMessage}>
                Empieza a buscar tus canciones favoritas
              </Text>
            );
          } else if (!loading && !isSearching && filteredResults.length === 0) {
            return (
              <View style={styles.noResultsContainer}>
                <Text style={styles.noResults}>
                  No hay resultados para "{searchTerm}"
                </Text>
                <TouchableOpacity
                  style={styles.suggestionButton}
                  onPress={() => navigation.navigate("SugerenciasScreen")}
                >
                  <Text style={styles.suggestionText}>Crear sugerencia</Text>
                </TouchableOpacity>
                {results.length > 0 && (
                  <Text style={styles.infoText}>
                    Es posible que la canción que buscas ya se encuentre en la
                    playlist.
                  </Text>
                )}
              </View>
            );
          } else {
            return null;
          }
        }}
        ListFooterComponent={
          hasMore && !loading ? (
            <TouchableOpacity
              style={styles.loadMoreButton}
              onPress={handleLoadMore}
            >
              <Text style={styles.loadMoreText}>Cargar más</Text>
            </TouchableOpacity>
          ) : loading ? (
            <ActivityIndicator size="large" color={Colors.PRIMARY_COLOR} />
          ) : null
        }
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#222",
    padding: 20,
  },
  title: {
    fontSize: 22,
    color: "#fff",
    fontWeight: "bold",
    marginBottom: 10,
  },
  searchContainer: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 15,
  },
  input: {
    backgroundColor: "#333",
    color: "#fff",
    padding: 10,
    borderRadius: 8,
    flex: 1,
  },
  searchButton: {
    marginLeft: 10,
    padding: 10,
    borderRadius: 8,
    backgroundColor: Colors.PRIMARY_COLOR,
  },
  songItem: {
    flexDirection: "row",
    justifyContent: "space-between",
    backgroundColor: "#444",
    padding: 12,
    borderRadius: 8,
    marginBottom: 10,
    alignItems: "center",
  },
  songText: {
    color: "#fff",
    fontSize: 16,
    maxWidth: 310,
  },
  noResults: {
    color: "#aaa",
    textAlign: "center",
    marginTop: 20,
  },
  infoText: {
    marginTop: 30,
    fontSize: 14,
    color: "#888",
    textAlign: "center",
    maxWidth: 340,
  },
  loadMoreButton: {
    backgroundColor: Colors.PRIMARY_COLOR,
    paddingVertical: 10,
    paddingHorizontal: 20,
    borderRadius: 8,
    alignSelf: "center",
    marginBottom: 20,
    marginTop: 20,
  },
  loadMoreText: {
    color: "#fff",
    fontSize: 16,
    fontWeight: "bold",
  },
  startMessage: {
    color: "#aaa",
    textAlign: "center",
    fontSize: 16,
    marginTop: 50,
  },

  noResultsContainer: {
    alignItems: "center",
    marginTop: 30,
  },

  suggestionButton: {
    backgroundColor: Colors.PRIMARY_COLOR,
    paddingVertical: 10,
    paddingHorizontal: 20,
    borderRadius: 8,
    marginTop: 10,
  },

  suggestionText: {
    color: "#fff",
    fontWeight: "bold",
    fontSize: 16,
  },
});

export default AddSongsScreen;
