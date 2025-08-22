import React, { useCallback, useEffect, useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  ActivityIndicator,
  TouchableOpacity,
} from "react-native";
import PlaylistGrid from "../components/PlaylistGrid"; // Importa el componente PlaylistGrid
import { getAllPlaylists } from "../services/apiService"; // Asegúrate de tener una función que obtenga las playlists públicas
import { useFocusEffect } from "@react-navigation/native";
import Colors from "../services/colors";

const PublicPlaylistsScreen = () => {
  const [playlists, setPlaylists] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null); // Para manejar errores
  const [offset, setOffset] = useState(0); // Offset para la paginación
  const [hasMore, setHasMore] = useState(false); // Para saber si hay más playlists
  const [loadingMore, setLoadingMore] = useState(false); // Para gestionar la carga de más playlists
  const limit = 10;

  const getPlaylists = async () => {
    setLoading(true);
    try {
      const playlistsData = await getAllPlaylists(offset, limit); // Llama al servicio para obtener las playlists públicas con el offset
      if (playlistsData.playlists.length === 0 && offset === 0) {
        setError("No se encontraron playlists públicas.");
      } else {
        setError(null);
      }

      // Filtra duplicados si se reciben las mismas playlists
      setPlaylists((prevPlaylists) => {
        const newPlaylists = playlistsData.playlists.filter(
          (newPlaylist) =>
            !prevPlaylists.some(
              (prevPlaylist) => prevPlaylist.id === newPlaylist.id
            )
        );
        return [...prevPlaylists, ...newPlaylists];
      });

      setHasMore(playlistsData.has_more); // Si hay más playlists, ajustamos hasMore
      setOffset((prevOffset) => prevOffset + playlistsData.playlists.length); // Actualizamos el offset con la cantidad de playlists que ya cargamos
    } catch (err) {
      console.error("Error al obtener playlists públicas:", err);
      setError("Error al cargar las playlists.");
    } finally {
      setLoading(false); // Cambia el estado de carga
      setLoadingMore(false); // Restablece la bandera de carga al cargar más
    }
  };

  // Usamos useEffect para cargar las playlists iniciales al montar la pantalla
  useEffect(() => {
    getPlaylists(); // Llamamos a la función al cargar la pantalla
  }, []);

  // Cuando el usuario regresa a la pantalla, recargamos las playlists
  useFocusEffect(
    useCallback(() => {
      setPlaylists([]); // Limpiamos las playlists para recargar
      setOffset(0); // Restablecemos el offset
      setHasMore(false); // Reiniciamos hasMore
      getPlaylists(); // Llamamos nuevamente a la función
    }, []) // Dependencias vacías para ejecutarse solo cuando se enfoque
  );

  // Función para cargar más playlists cuando el usuario hace clic en "Cargar más"
  const loadMorePlaylists = () => {
    if (hasMore && !loadingMore) {
      setLoadingMore(true);
      getPlaylists();
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Playlists Públicas</Text>
      {loading ? (
        <ActivityIndicator size="large" color={Colors.PRIMARY_COLOR} />
      ) : error ? (
        <Text style={styles.errorText}>{error}</Text> // Muestra un mensaje de error si no se pudieron cargar las playlists
      ) : (
        <>
          <PlaylistGrid playlists={playlists} showUser={true} />
          {hasMore && (
            <TouchableOpacity
              style={[
                styles.loadMoreButton,
                loadingMore && styles.loadingButton,
              ]}
              onPress={loadMorePlaylists}
              disabled={loadingMore}
            >
              <Text style={styles.loadMoreText}>
                {loadingMore ? "Cargando..." : "Cargar más"}
              </Text>
            </TouchableOpacity>
          )}
        </>
      )}
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
  loadMoreButton: {
    marginTop: 20,
    backgroundColor: Colors.PRIMARY_COLOR, // Color verde de Spotify
    paddingVertical: 12,
    paddingHorizontal: 25,
    borderRadius: 30,
    alignItems: "center",
    justifyContent: "center",
    elevation: 5,
  },
  loadingButton: {
    backgroundColor: "#333", // Cambiar el color cuando se está cargando
  },
  loadMoreText: {
    color: "#fff",
    fontSize: 18,
    fontWeight: "bold",
  },
});

export default PublicPlaylistsScreen;
