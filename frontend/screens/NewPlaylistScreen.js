import { useState } from "react";
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  Switch,
} from "react-native";
import { createPlaylist, getLoggedUserId } from "../services/apiService";
import { emitPlaylistsModifiedEvent } from "../events/playlistsModifiedEvent"; // Importa el evento
import Colors from "../services/colors";
import { useAlert } from "../services/alertContext";

const NewPlaylistScreen = ({ navigation }) => {
  const { showAlert } = useAlert();

  const [playlistName, setPlaylistName] = useState("");
  const [isPublic, setIsPublic] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    if (loading) return;

    setLoading(true);

    try {
      const success = await createPlaylist(
        await getLoggedUserId(),
        playlistName,
        isPublic
      );
      if (success) {
        emitPlaylistsModifiedEvent(); // Emitir el evento
        navigation.goBack(); // Volver a la pantalla anterior (UserPlaylistsScreen)
      } else {
        useAlert(
          "Error",
          "Hubo un error al crear la playlist. Intenta nuevamente."
        );
      }
    } catch (error) {
      console.error("Error creating playlist:", error);
      useAlert(
        "Error",
        "Hubo un error al crear la playlist. Intenta nuevamente."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Crear Nueva Playlist</Text>

      <TextInput
        style={styles.input}
        placeholder="Nombre de la Playlist"
        value={playlistName}
        onChangeText={setPlaylistName}
        autoCapitalize="words"
        placeholderTextColor="#bbb" // Color del texto del placeholder
      />

      <View style={styles.switchContainer}>
        <Text style={styles.label}>¿Hacerla pública?</Text>
        <Switch
          value={isPublic}
          onValueChange={setIsPublic}
          trackColor={{ false: "#767577", true: Colors.SECONDARY_COLOR }}
          thumbColor={isPublic ? Colors.PRIMARY_COLOR : "#f4f3f4"}
        />
      </View>

      <TouchableOpacity
        style={[styles.button, !playlistName.trim() && styles.buttonDisabled]}
        onPress={handleSubmit}
        disabled={loading || !playlistName.trim()}
      >
        <Text
          style={[
            styles.buttonText,
            !playlistName.trim() && styles.buttonTextDisabled,
          ]}
        >
          {loading ? "Creando..." : "Crear Playlist"}
        </Text>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    backgroundColor: "#121212", // Fondo más oscuro
    justifyContent: "center",
  },
  title: {
    fontSize: 28, // Tamaño de fuente mayor para el título
    color: "white",
    marginBottom: 30,
    textAlign: "center",
    fontWeight: "bold",
  },
  input: {
    backgroundColor: "#333",
    color: "white",
    padding: 15,
    borderRadius: 10, // Bordes más suaves
    marginBottom: 20,
    fontSize: 16,
  },
  switchContainer: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 30, // Espaciado mejorado
  },
  label: {
    color: "white",
    fontSize: 16,
    marginRight: 10,
  },
  button: {
    backgroundColor: Colors.PRIMARY_COLOR, // Verde característico de Spotify
    paddingVertical: 15,
    paddingHorizontal: 30,
    borderRadius: 30, // Bordes redondeados
    alignItems: "center",
    elevation: 5,
    shadowColor: "#000", // Sombra para dar profundidad
    shadowOffset: { width: 0, height: 3 },
    shadowOpacity: 0.2,
    shadowRadius: 4,
  },
  buttonDisabled: {
    backgroundColor: "#555", // Gris cuando está deshabilitado
  },
  buttonText: {
    color: "white",
    fontWeight: "bold",
    fontSize: 18, // Texto más grande para el botón
  },
  buttonTextDisabled: {
    color: "#888", // Texto más gris cuando está deshabilitado
  },
});

export default NewPlaylistScreen;
