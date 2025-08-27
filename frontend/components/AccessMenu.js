import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  TextInput,
  Modal,
  Image,
  Linking,
} from "react-native";
import LoginMenu from "./LoginMenu";
import { getApiBaseUrl, login, setApiBaseUrl } from "../services/apiService";
import { useAlert } from "../services/alertContext";
import Colors from "../services/colors";
import { useEffect, useState } from "react";
import playbuttonLogo from "../assets/playbutton-logo.png";

const AccessMenu = ({ onLoginSuccess }) => {
  const { showAlert } = useAlert();
  const [showConfig, setShowConfig] = useState(false);
  const [apiUrl, setApiUrl] = useState("");

  const handleLoginSubmit = async (email, password) => {
    const url = await getApiBaseUrl();
    console.log(url);
    if (!url) throw new Error("Configura la URL del servidor");

    try {
      await login(email, password);
      onLoginSuccess();
    } catch (err) {
      throw new Error(err.message);
    }
  };

  useEffect(() => {
    (async () => {
      const savedUrl = await getApiBaseUrl();
      setApiUrl(savedUrl);
    })();
  }, []);

  const handleSaveUrl = async () => {
    url = await setApiBaseUrl(apiUrl);
    setApiUrl(url);
    setShowConfig(false);
  };

  const openGitHub = () => {
    showAlert(
      "Abrir repositorio en GitHub",
      "En el repositorio oficial encontrarás la información para desplegar tu propio servidor de PlayButton. Esta acción abrirá el navegador del dispositivo.",
      [
        { text: "Cancelar", style: "cancel" },
        {
          text: "Abrir",
          onPress: async () => {
            Linking.openURL("https://github.com/DanielPerezL/PlayButton");
          },
        },
      ]
    );
  };

  return (
    <View style={styles.container}>
      <Image source={playbuttonLogo} style={styles.logo} resizeMode="contain" />

      <View style={styles.menu}>
        <Text style={styles.title}>Iniciar Sesión</Text>

        <LoginMenu onSubmit={handleLoginSubmit} />

        <TouchableOpacity
          onPress={() =>
            showAlert(
              "¿No tienes cuenta?",
              "Para acceder necesitas una cuenta en el servidor de tu comunidad. Contacta con el administrador de tu servidor para crearla. " +
                "Si quieres desplegar tu propio servidor de PlayButton, encontrarás toda la información en el repositorio oficial de GitHub."
            )
          }
        >
          <Text style={styles.link}>¿No tienes cuenta?</Text>
        </TouchableOpacity>

        <TouchableOpacity onPress={() => setShowConfig(true)}>
          <Text style={[styles.link, { marginTop: 10 }]}>
            Configurar servidor
          </Text>
        </TouchableOpacity>
        <View style={styles.separator} />
        <TouchableOpacity onPress={openGitHub}>
          <Text style={[styles.link, { marginTop: 10 }]}>Ver en GitHub</Text>
        </TouchableOpacity>
      </View>

      <Modal visible={showConfig} animationType="fade" transparent={true}>
        <View style={styles.modalOverlay}>
          <View style={styles.modalBox}>
            <Text style={styles.modalTitle}>Configurar URL del servidor</Text>
            <TextInput
              style={styles.modalInput}
              value={apiUrl}
              onChangeText={setApiUrl}
              placeholder="Introduce el dominio o IP del servidor..."
              placeholderTextColor="#aaa"
              autoCapitalize="none"
            />
            <View style={styles.modalButtons}>
              <TouchableOpacity
                style={[styles.modalButton, styles.cancelButton]}
                onPress={() => setShowConfig(false)}
              >
                <Text style={styles.modalButtonText}>Cancelar</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.modalButton}
                onPress={handleSaveUrl}
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

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
    justifyContent: "center",
    alignItems: "center",
  },
  menu: {
    width: "100%",
    maxWidth: 400,
    padding: 24,
    backgroundColor: "#333",
    borderRadius: 12,
    elevation: 4,
  },
  title: {
    fontSize: 24,
    textAlign: "center",
    marginBottom: 16,
    color: "#fff",
  },
  link: {
    marginTop: 20,
    color: Colors.PRIMARY_COLOR,
    textAlign: "center",
  },
  logo: {
    width: "90%",
    maxWidth: 400,
    maxHeight: 100,
    alignSelf: "center",
    marginBottom: 40,
  },
  separator: {
    height: 2,
    backgroundColor: "#444",
    marginTop: 10,
    width: "100%",
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: "rgba(0,0,0,0.7)",
    justifyContent: "center",
    alignItems: "center",
    padding: 20,
  },
  modalBox: {
    backgroundColor: "#2a2a2a",
    borderRadius: 14,
    padding: 20,
    width: "100%",
    maxWidth: 400,
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: "bold",
    color: "white",
    marginBottom: 15,
  },
  modalInput: {
    borderWidth: 1,
    borderColor: "#444",
    padding: 10,
    borderRadius: 8,
    backgroundColor: "#1f1f1f",
    color: "white",
    marginBottom: 20,
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
    color: "white",
    fontWeight: "bold",
    fontSize: 16,
  },
});

export default AccessMenu;
