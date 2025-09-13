import { useCallback, useEffect, useRef, useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  ActivityIndicator,
  TouchableOpacity,
} from "react-native";
import { useFocusEffect, useNavigation } from "@react-navigation/native";
import PlaylistGrid from "../components/PlaylistGrid";
import { fetchUserPlaylists, getLoggedUserId } from "../services/apiService";
import { Ionicons } from "@expo/vector-icons";
import {
  subscribeToPlaylistsModifiedEvent,
  unsubscribeFromPlaylistsModifiedEvent,
} from "../events/playlistsModifiedEvent"; // Importa las funciones
import Colors from "../services/colors";

const UserPlaylistsScreen = () => {
  const [playlists, setPlaylists] = useState([]);
  const playlistsRef = useRef([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigation = useNavigation();

  useEffect(() => {
    playlistsRef.current = playlists;
  }, [playlists]);

  const getPlaylists = async (force = false) => {
    // Si ya tenemos playlists cargadas, no hacemos nada
    if (!force && playlistsRef.current.length != 0) {
      return;
    }

    setLoading(true);
    try {
      const userId = await getLoggedUserId();
      const playlistsData = await fetchUserPlaylists(userId);
      if (playlistsData.length === 0) {
        setError("No tienes playlists aún.");
      } else {
        setError(null);
      }

      setPlaylists(playlistsData);
    } catch (err) {
      console.error("Error al obtener playlists:", err);
      setError("Error al obtener las playlists.");
    } finally {
      setLoading(false);
    }
  };

  // Escuchar el evento de la creación de playlist
  useEffect(() => {
    const listener = subscribeToPlaylistsModifiedEvent(() => {
      getPlaylists(true); // Actualizar las playlists cuando se crea una nueva
    });

    // Limpiar el listener cuando el componente se desmonte
    return () => {
      unsubscribeFromPlaylistsModifiedEvent(listener);
    };
  }, []);

  useFocusEffect(
    useCallback(() => {
      getPlaylists();
    }, [])
  );

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Mis Playlists</Text>
      {loading ? (
        <ActivityIndicator size="large" color={Colors.PRIMARY_COLOR} />
      ) : error ? (
        <Text style={styles.errorText}>{error}</Text>
      ) : (
        <PlaylistGrid
          playlists={playlists}
          showUser={false}
          onRefreshPlaylists={getPlaylists}
        />
      )}

      <TouchableOpacity
        style={styles.fab}
        onPress={() => navigation.navigate("NewPlaylistScreen")}
      >
        <Ionicons name="add" size={32} color="#fff" />
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    backgroundColor: "#121212", // Fondo más oscuro
  },
  title: {
    fontSize: 26, // Un tamaño de fuente más grande
    color: "#fff",
    marginBottom: 20,
    textAlign: "center",
    fontWeight: "bold",
  },
  errorText: {
    color: "#aaa", // Color más suave y amigable para los errores
    fontSize: 16,
    textAlign: "center",
    fontStyle: "italic", // Estilo itálico para el mensaje de error
  },
  fab: {
    position: "absolute",
    right: 20,
    bottom: 30,
    backgroundColor: Colors.PRIMARY_COLOR, // Color verde de Spotify
    borderRadius: 30,
    padding: 15,
    elevation: 8, // Añadimos sombra más prominente
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.15,
    shadowRadius: 5,
    transform: [{ scale: 1 }],
    transition: "transform 0.2s ease-in-out", // Transición suave para el hover
  },
  fabHovered: {
    transform: [{ scale: 1.1 }], // Efecto de hover
  },
});

export default UserPlaylistsScreen;
