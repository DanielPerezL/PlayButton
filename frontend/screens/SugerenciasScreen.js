import React, { useState } from "react";
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
} from "react-native";
import { createSuggestion } from "../services/apiService"; // Asegúrate de que el path sea correcto
import Colors from "../services/colors";
import { useAlert } from "../services/alertContext";

const SugerenciasScreen = () => {
  const { showAlert } = useAlert();

  const [artistName, setArtistName] = useState("");
  const [songName, setSongName] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    if (!artistName.trim() || !songName.trim()) {
      showAlert("Por favor, completa ambos campos.");
      return;
    }

    setLoading(true);
    try {
      const success = await createSuggestion(songName, artistName);
      setLoading(false);

      if (success) {
        showAlert(
          "Sugerencia enviada",
          "Hemos recibido tu sugerencia correctamente. Pronto podrás encontrar esa canción en el servidor actual."
        );
        setArtistName("");
        setSongName("");
      } else {
        showAlert(
          "Error inesperado",
          "Hubo un error al enviar la sugerencia. Intenta nuevamente."
        );
      }
    } catch (error) {
      setLoading(false);
      showAlert("Error", "Hubo un error de conexión, intenta nuevamente.");
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Sugerir canción</Text>

      <TextInput
        placeholder="Nombre del artista"
        value={artistName}
        onChangeText={setArtistName}
        style={styles.input}
        placeholderTextColor="#aaa"
      />

      <TextInput
        placeholder="Nombre de la canción"
        value={songName}
        onChangeText={setSongName}
        style={styles.input}
        placeholderTextColor="#aaa"
      />

      <TouchableOpacity
        style={[
          styles.button,
          (loading || !artistName.trim() || !songName.trim()) &&
            styles.buttonDisabled, // Estilo deshabilitado
        ]}
        onPress={handleSubmit}
        disabled={loading || !artistName.trim() || !songName.trim()}
      >
        {loading ? (
          <ActivityIndicator size="small" color="#fff" /> // Indicador de carga
        ) : (
          <Text
            style={[
              styles.buttonText,
              (loading || !artistName.trim() || !songName.trim()) &&
                styles.buttonTextDisabled, // Estilo del texto deshabilitado
            ]}
          >
            Enviar sugerencia
          </Text>
        )}
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 25,
    backgroundColor: "#222",
    justifyContent: "center",
  },
  title: {
    fontSize: 26,
    color: "white",
    marginBottom: 20,
    textAlign: "center",
    fontWeight: "bold",
  },
  input: {
    backgroundColor: "#333",
    color: "white",
    padding: 15,
    borderRadius: 8,
    marginBottom: 15,
    fontSize: 16,
  },
  button: {
    backgroundColor: Colors.PRIMARY_COLOR, // Color verde para el botón
    padding: 15,
    borderRadius: 30,
    alignItems: "center",
    flexDirection: "row", // Para alinear el texto y el indicador de carga
    justifyContent: "center",
  },
  buttonDisabled: {
    backgroundColor: "#cccccc", // Color cuando está deshabilitado
  },
  buttonText: {
    color: "white",
    fontWeight: "bold",
    fontSize: 16,
  },
  buttonTextDisabled: {
    color: "#888888", // Texto gris cuando está deshabilitado
  },
});

export default SugerenciasScreen;
