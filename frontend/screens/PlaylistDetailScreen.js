import React, { useEffect, useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  FlatList,
  ActivityIndicator,
  Modal,
  TextInput,
} from "react-native";
import { useNavigation, useRoute } from "@react-navigation/native";
import { Ionicons } from "@expo/vector-icons";
import { Switch } from "react-native";
import {
  getCachedSongs,
  getPlaylistSongs,
  removeSongFromPlaylist,
  setCachedSongs,
  getLoggedUserId,
  updatePlaylist,
  deletePlaylist,
  getIsAdmin,
} from "../services/apiService";
import { emitPlaylistsModifiedEvent } from "../events/playlistsModifiedEvent";
import Colors from "../services/colors";
import { useAlert } from "../services/alertContext";

const PlaylistDetailScreen = () => {
  const { showAlert } = useAlert();

  const route = useRoute();
  const navigation = useNavigation();
  const { playlist } = route.params;

  const [songs, setSongs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [isOwner, setIsOwner] = useState(false);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [newPlaylistName, setNewPlaylistName] = useState(playlist.name);
  const [isPublic, setIsPublic] = useState(playlist.is_public);
  const [searchTerm, setSearchTerm] = useState("");
  const [showActions, setShowActions] = useState(false);

  const fetchSongs = async (forceRefresh = false) => {
    try {
      const userId = await getLoggedUserId();
      const isOwnerUser = userId === playlist.user_id || getIsAdmin();
      setIsOwner(isOwnerUser);

      if (!forceRefresh && isOwnerUser) {
        const cached = await getCachedSongs(playlist.id);
        if (cached) {
          setSongs(cached);
          setLoading(false);
          return;
        }
      }

      const fetchedSongs = await getPlaylistSongs(playlist.id);
      if (fetchSongs === null) {
        throw new Error("Error obteniendo canciones de la playlist");
      }
      setSongs(fetchedSongs);

      if (isOwnerUser) {
        await setCachedSongs(playlist.id, fetchedSongs);
      }
      setError(false);
    } catch (err) {
      console.error("Error fetching songs:", err);
      setSongs([]);
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const unsubscribe = navigation.addListener("focus", () => {
      fetchSongs();
    });

    return unsubscribe;
  }, [playlist.id, navigation, route.params]);

  const handlePlay = () => {
    if (songs.length > 0) {
      navigation.navigate("Reproductor", {
        songs: songs,
        playlist_name: playlist.name,
      });
    }
  };

  const handleDeleteSong = (songId) => {
    showAlert(
      "Eliminar canción",
      "¿Estás seguro de que quieres eliminar esta canción de la playlist?",
      [
        { text: "Cancelar", style: "cancel" },
        {
          text: "Eliminar",
          style: "destructive",
          onPress: async () => {
            try {
              await removeSongFromPlaylist(playlist.id, songId);
              const updated = songs.filter((song) => song.id !== songId);
              setSongs(updated);
              await setCachedSongs(playlist.id, updated);
            } catch (err) {
              console.error("Error al eliminar canción:", err);
            }
          },
        },
      ]
    );
  };

  const filteredSongs =
    songs &&
    songs.filter((song) =>
      song.name?.toLowerCase().includes(searchTerm.toLowerCase())
    );
  const renderSongItem = ({ item }) => {
    let artist = "Desconocido";
    let title = item.name;

    if (item.name && item.name.includes(" - ")) {
      [artist, title] = item.name.split(" - ");
    }

    return (
      <View style={styles.songItem}>
        <View style={{ flex: 1 }}>
          <Text style={styles.songTitle}>{title}</Text>
          <Text style={styles.songArtist}>{artist}</Text>
        </View>
        {isOwner && (
          <TouchableOpacity
            onPress={() => handleDeleteSong(item.id)}
            style={styles.deleteButton}
          >
            <Ionicons name="trash" size={20} color={Colors.ERROR_COLOR} />
          </TouchableOpacity>
        )}
      </View>
    );
  };

  return (
    <View style={styles.container}>
      <View style={styles.titleWrapper}>
        <Ionicons
          name="musical-notes"
          size={28}
          color="#fff"
          style={styles.titleIcon}
        />
        <Text style={styles.title}>{playlist.name}</Text>
      </View>
      <View style={styles.subtitleRow}>
        <Ionicons
          name="person-circle"
          size={18}
          color="#aaa"
          style={{ marginRight: 6 }}
        />
        <Text style={styles.subtitle}>{playlist.user}</Text>
      </View>
      <Text style={styles.subtitle}>
        {playlist.is_public ? "Pública" : "Privada"} · {songs.length} canciones
      </Text>
      <View style={styles.searchContainer}>
        <Ionicons
          name="search"
          size={20}
          color="#aaa"
          style={styles.searchIcon}
        />
        <TextInput
          style={styles.searchInput}
          placeholder="Buscar canción en la playlist"
          placeholderTextColor="#aaa"
          value={searchTerm}
          onChangeText={setSearchTerm}
        />
        {searchTerm.length > 0 && (
          <TouchableOpacity onPress={() => setSearchTerm("")}>
            <Ionicons
              name="close-circle"
              size={20}
              color="#aaa"
              style={styles.clearIcon}
            />
          </TouchableOpacity>
        )}
      </View>

      {isOwner && (
        <View>
          <View
            style={{
              flexDirection: "row",
              gap: 10,
            }}
          >
            <TouchableOpacity
              style={styles.addButton}
              onPress={() =>
                navigation.navigate("AddSongsScreen", {
                  ...route.params,
                  playlistId: playlist.id,
                })
              }
            >
              <Ionicons name="add" size={20} color="white" />
              <Text style={styles.addButtonText}>Añadir canciones</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[
                styles.addButton,
                {
                  backgroundColor: Colors.SECONDARY_COLOR,
                  flex: 1,
                },
              ]}
              onPress={() => setShowActions(!showActions)}
            >
              <Ionicons
                name={showActions ? "chevron-up" : "chevron-down"}
                size={20}
                color="white"
              />
              <Text style={styles.addButtonText}>Opciones</Text>
            </TouchableOpacity>
          </View>

          {showActions && (
            <View style={{ flexDirection: "row", gap: 10 }}>
              <TouchableOpacity
                style={[
                  styles.addButton,
                  { backgroundColor: Colors.SECONDARY_COLOR, flex: 1 },
                ]}
                onPress={() => setEditModalVisible(true)}
              >
                <Ionicons name="create" size={20} color="white" />
                <Text style={styles.addButtonText}>Editar</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[
                  styles.addButton,
                  { backgroundColor: "#dc3545", flex: 1 },
                ]}
                onPress={() => {
                  showAlert(
                    "Eliminar Playlist",
                    "¿Estás seguro de que quieres eliminar esta playlist?",
                    [
                      {
                        text: "Cancelar",
                        style: "cancel",
                      },
                      {
                        text: "Eliminar",
                        style: "destructive",
                        onPress: async () => {
                          const success = await deletePlaylist(playlist.id);
                          if (success) {
                            emitPlaylistsModifiedEvent();
                            navigation.goBack();
                          } else {
                            showAlert("Error al eliminar la playlist");
                          }
                        },
                      },
                    ]
                  );
                }}
              >
                <Ionicons name="trash" size={20} color="white" />
                <Text style={styles.addButtonText}>Eliminar</Text>
              </TouchableOpacity>
            </View>
          )}
        </View>
      )}

      <View style={styles.listContainer}>
        {loading ? (
          <ActivityIndicator size="large" color={Colors.PRIMARY_COLOR} />
        ) : error ? (
          <Text style={styles.emptyText}>
            Ocurrió un error al cargar las canciones."
          </Text>
        ) : songs.length === 0 ? (
          <Text style={styles.emptyText}>
            Esta playlist no tiene canciones.
          </Text>
        ) : (
          <FlatList
            data={filteredSongs}
            renderItem={renderSongItem}
            keyExtractor={(item) => item.id.toString()}
          />
        )}
      </View>

      <TouchableOpacity
        style={[styles.playButton, songs.length === 0 && styles.buttonDisabled]}
        onPress={handlePlay}
        disabled={songs.length === 0}
      >
        <Ionicons
          name="play"
          size={20}
          color={songs.length === 0 ? "#888" : "#fff"}
          style={{ marginRight: 10 }}
        />
        <Text
          style={[
            styles.buttonText,
            songs.length === 0 && styles.buttonTextDisabled,
          ]}
        >
          Reproducir
        </Text>
      </TouchableOpacity>

      {/* Modal para editar nombre de playlist */}
      <Modal
        visible={editModalVisible}
        animationType="slide"
        transparent={true}
        onRequestClose={() => setEditModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Editar playlist</Text>
            <TextInput
              style={styles.input}
              value={newPlaylistName}
              onChangeText={setNewPlaylistName}
              placeholder="Nuevo nombre"
              placeholderTextColor="#aaa"
            />
            <View
              style={{
                flexDirection: "row",
                alignItems: "center",
                marginBottom: 10,
              }}
            >
              <Text style={{ color: "white", marginRight: 10 }}>¿Pública?</Text>
              <Switch
                value={isPublic}
                onValueChange={setIsPublic}
                trackColor={{ false: "#555", true: Colors.PRIMARY_COLOR }}
                thumbColor={isPublic ? "#fff" : "#ccc"}
              />
            </View>

            <View style={styles.modalButtons}>
              <TouchableOpacity
                style={[styles.modalButton, { backgroundColor: "#555" }]}
                onPress={() => setEditModalVisible(false)}
              >
                <Text style={styles.modalButtonText}>Cancelar</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[
                  styles.modalButton,
                  { backgroundColor: Colors.PRIMARY_COLOR },
                ]}
                onPress={async () => {
                  if (!newPlaylistName.trim()) return;
                  const success = await updatePlaylist(
                    playlist.id,
                    newPlaylistName.trim(),
                    isPublic
                  );
                  if (success) {
                    emitPlaylistsModifiedEvent();
                    playlist.name = newPlaylistName.trim(); // actualizar localmente
                    playlist.is_public = isPublic;
                    setEditModalVisible(false);
                  } else {
                    showAlert("Error al actualizar");
                  }
                }}
              >
                <Text style={styles.modalButtonText}>Guardar</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </View>
  );
};

// Solo muestro los estilos y pequeños ajustes en JSX, ya que tu lógica está perfecta.
// Reemplaza tu objeto `styles` por este actualizado:
const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#1c1c1c",
    padding: 20,
  },
  title: {
    color: "white",
    fontSize: 28,
    fontWeight: "bold",
    marginBottom: 5,
    flexDirection: "row",
  },
  titleWrapper: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 10,
  },
  titleIcon: {
    marginRight: 10,
  },
  subtitle: {
    color: "#aaa",
    fontSize: 16,
    marginBottom: 3,
  },
  subtitleRow: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 3,
  },
  listContainer: {
    flex: 1,
    marginTop: 20,
  },
  emptyText: {
    color: "#888",
    fontSize: 16,
    textAlign: "center",
    marginTop: 40,
  },
  songItem: {
    backgroundColor: "#2a2a2a",
    padding: 16,
    borderRadius: 14,
    marginBottom: 12,
    flexDirection: "row",
    alignItems: "center",
    elevation: 2,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 4,
  },
  songTitle: {
    color: "white",
    fontSize: 16,
    fontWeight: "bold",
  },
  songArtist: {
    color: "#aaa",
    fontSize: 14,
    marginTop: 2,
  },
  deleteButton: {
    marginLeft: 15,
  },
  addButton: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: Colors.PRIMARY_COLOR,
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 12,
    alignSelf: "flex-start",
    marginTop: 10,
    elevation: 3,
  },
  addButtonText: {
    color: "white",
    fontSize: 16,
    marginLeft: 8,
    fontWeight: "600",
  },
  playButton: {
    backgroundColor: Colors.PRIMARY_COLOR,
    paddingVertical: 16,
    paddingHorizontal: 25,
    borderRadius: 14,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    marginTop: 25,
    elevation: 3,
  },
  buttonDisabled: {
    backgroundColor: "#444",
  },
  buttonText: {
    color: "white",
    fontSize: 18,
    fontWeight: "bold",
  },
  buttonTextDisabled: {
    color: "#aaa",
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: "rgba(0,0,0,0.7)",
    justifyContent: "center",
    alignItems: "center",
    padding: 20,
  },
  modalContent: {
    backgroundColor: "#2a2a2a",
    padding: 20,
    borderRadius: 14,
    width: "100%",
    maxWidth: 400,
    shadowColor: "#000",
    shadowOpacity: 0.5,
    shadowRadius: 10,
    elevation: 10,
  },
  modalTitle: {
    color: "white",
    fontSize: 20,
    fontWeight: "bold",
    marginBottom: 15,
  },
  input: {
    backgroundColor: "#1f1f1f",
    borderWidth: 1,
    borderColor: "#444",
    color: "white",
    padding: 12,
    borderRadius: 8,
    fontSize: 16,
    marginBottom: 15,
  },
  modalButtons: {
    flexDirection: "row",
    justifyContent: "flex-end",
    gap: 10,
  },
  modalButton: {
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderRadius: 10,
    backgroundColor: Colors.PRIMARY_COLOR,
  },
  cancelButton: {
    backgroundColor: "#555",
  },
  modalButtonText: {
    color: "#fff",
    fontWeight: "bold",
    fontSize: 16,
  },
  searchContainer: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "#2a2a2a",
    borderRadius: 10,
    paddingHorizontal: 10,
    marginTop: 5,
  },
  searchIcon: {
    marginRight: 8,
  },
  searchInput: {
    flex: 1,
    color: "#fff",
    fontSize: 16,
    paddingVertical: 10,
  },
  clearIcon: {
    marginLeft: 8,
  },
});

export default PlaylistDetailScreen;
