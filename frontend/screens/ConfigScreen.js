import { useEffect, useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  Linking,
  Modal,
  TextInput,
  ActivityIndicator,
} from "react-native";
import Constants from "expo-constants";
import Colors from "../services/colors";
import {
  getApiBaseUrl,
  changeUserPassword,
  deleteUserAccount,
  logout,
} from "../services/apiService";
import { useAlert } from "../services/alertContext";
import { useNavigation } from "@react-navigation/native";

const ConfigScreen = () => {
  const navigation = useNavigation();
  const { showAlert } = useAlert();
  const [apiUrl, setApiUrl] = useState("");

  // Estados para el Modal
  const [modalVisible, setModalVisible] = useState(false);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    const fetchApiUrl = async () => {
      const url = await getApiBaseUrl();
      setApiUrl(url);
    };
    fetchApiUrl();
  }, []);

  const handlePasswordChange = async () => {
    if (!currentPassword || !newPassword || !confirmPassword) {
      showAlert("Error", "Por favor, rellena todos los campos.");
      return;
    }

    if (newPassword !== confirmPassword) {
      showAlert("Error", "Las nuevas contraseñas no coinciden.");
      return;
    }

    if (newPassword.length < 6) {
      showAlert("Error", "La contraseña debe tener al menos 6 caracteres.");
      return;
    }

    setIsSubmitting(true);
    success = await changeUserPassword(currentPassword, newPassword);
    setIsSubmitting(false);
    console.log("Password change success:", success);
    // Limpiar campos
    setCurrentPassword("");
    setNewPassword("");
    setConfirmPassword("");

    if (success) {
      showAlert("Éxito", "Contraseña actualizada correctamente.");
      setModalVisible(false);
    } else {
      showAlert(
        "Error",
        "No se pudo cambiar la contraseña. Comprueba tu contraseña actual e inténtalo de nuevo.",
      );
    }
  };

  const handleDeleteAccount = () => {
    showAlert(
      "¡Atención!",
      "¿Estás seguro de que quieres eliminar tu cuenta? Esta acción es permanente, perderás todas tus playlists y el acceso al servidor.",
      [
        { text: "Cancelar", style: "cancel" },
        {
          text: "Eliminar",
          style: "destructive",
          onPress: async () => {
            try {
              await deleteUserAccount();
              await logout();
              navigation.reset({
                index: 0,
                routes: [{ name: "LoginScreen" }],
              });
            } catch (error) {
              showAlert("Error", error.message);
            }
          },
        },
      ],
    );
  };

  const onPressItem = (item) => {
    if (item.action) {
      item.action();
      return;
    }

    if (!item.msg) return;

    const alertConfig = {
      title: item.msg.title,
      body: item.msg.body,
    };

    if (item.msg.link) {
      alertConfig.buttons = [
        { text: "Cerrar", style: "cancel" },
        { text: "Abrir enlace", onPress: () => Linking.openURL(item.msg.link) },
      ];
    }

    showAlert(alertConfig.title, alertConfig.body, alertConfig.buttons);
  };

  const renderConfigItem = ({ item }) => {
    const displayValue = item.value
      ? item.value.replace(/^https?:\/\//, "")
      : "";
    const toucheable = item.msg || item.action;

    return (
      <TouchableOpacity
        onPress={toucheable ? () => onPressItem(item) : undefined}
        style={[styles.item, item.isDanger && styles.dangerItem]}
        activeOpacity={toucheable ? 0.6 : 1}
      >
        <Text style={[styles.label, item.isDanger && styles.dangerLabel]}>
          {item.key}
        </Text>
        <Text style={[styles.value, item.isDanger && styles.dangerValue]}>
          {displayValue}
        </Text>
      </TouchableOpacity>
    );
  };
  // Datos de configuración que se mostrarán en la pantalla
  // "key" debe ser un identificador único para cada elemento
  const configItems = [
    // Servidor actual
    {
      key: "Servidor actual",
      value: apiUrl,
      msg: {
        title: "Info Servidor",
        body:
          "Esta es la URL actual de tu servidor, si necesitas " +
          "cambiarla debes cerrar sesión primero.",
      },
    },

    // Repo de GitHub
    {
      key: "Repositorio en GitHub",
      value: "https://github.com/DanielPerezL/PlayButton",
      msg: {
        title: "Repositorio en GitHub",
        body:
          "Este es el enlace al repositorio oficial del proyecto en GitHub. " +
          "Puedes visitarlo para ver el código fuente, contribuir o descubrir " +
          "como desplegar tu propio servidor de PlayButton.",
        link: "https://github.com/DanielPerezL/PlayButton",
      },
    },

    // Sugerir canciones
    {
      key: "Sugerencias",
      value: "Sugerir canciones",
      action: () => navigation.navigate("SugerenciasScreen"),
    },

    // Versión de app
    { key: "Versión de la app", value: Constants.expoConfig.version },

    // Cambiar contraseña
    {
      key: "Cambiar contraseña",
      value: "Seguridad de la cuenta",
      action: () => setModalVisible(true),
    },

    // Eliminar cuenta (zona de peligro)
    {
      key: "Zona de peligro",
      value: "Eliminar cuenta permanentemente",
      isDanger: true,
      action: handleDeleteAccount,
    },
  ];

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Configuración</Text>

      <FlatList
        data={configItems}
        renderItem={renderConfigItem}
        keyExtractor={(item) => item.key}
        numColumns={1}
      />

      {/* MODAL DE CAMBIO DE CONTRASEÑA */}
      <Modal
        visible={modalVisible}
        animationType="slide"
        transparent={true}
        onRequestClose={() => setModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Cambiar Contraseña</Text>

            <TextInput
              style={styles.input}
              placeholder="Contraseña actual"
              placeholderTextColor="#777"
              secureTextEntry
              value={currentPassword}
              onChangeText={setCurrentPassword}
            />

            <TextInput
              style={styles.input}
              placeholder="Nueva contraseña"
              placeholderTextColor="#777"
              secureTextEntry
              value={newPassword}
              onChangeText={setNewPassword}
            />

            <TextInput
              style={styles.input}
              placeholder="Confirmar nueva contraseña"
              placeholderTextColor="#777"
              secureTextEntry
              value={confirmPassword}
              onChangeText={setConfirmPassword}
            />

            <View style={styles.modalButtons}>
              <TouchableOpacity
                style={[styles.modalButton, styles.cancelButton]}
                onPress={() => setModalVisible(false)}
              >
                <Text style={styles.buttonText}>Cancelar</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[styles.modalButton, styles.confirmButton]}
                onPress={handlePasswordChange}
                disabled={isSubmitting}
              >
                {isSubmitting ? (
                  <ActivityIndicator color="#fff" size="small" />
                ) : (
                  <Text style={styles.buttonText}>Guardar</Text>
                )}
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
    backgroundColor: "#121212",
    padding: 16,
  },
  title: {
    fontSize: 24,
    fontWeight: "bold",
    color: "#fff",
    marginBottom: 20,
    textAlign: "center",
  },
  item: {
    marginBottom: 15,
    padding: 15,
    backgroundColor: "#1f1f1f",
    borderRadius: 8,
  },
  label: {
    fontSize: 16,
    color: "#aaa",
    marginBottom: 5,
  },
  value: {
    fontSize: 18,
    fontWeight: "bold",
    color: "#fff",
  },
  button: {
    marginTop: 20,
    backgroundColor: Colors.PRIMARY_COLOR,
    padding: 12,
    borderRadius: 8,
    alignItems: "center",
  },
  buttonText: {
    color: "#fff",
    fontSize: 16,
    fontWeight: "bold",
  },
  // ESTILOS DEL MODAL
  modalOverlay: {
    flex: 1,
    backgroundColor: "rgba(0,0,0,0.8)",
    justifyContent: "center",
    padding: 20,
  },
  modalContent: {
    backgroundColor: "#1f1f1f",
    borderRadius: 12,
    padding: 20,
    borderWidth: 1,
    borderColor: "#333",
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: "bold",
    color: "#fff",
    marginBottom: 20,
    textAlign: "center",
  },
  input: {
    backgroundColor: "#2c2c2c",
    borderRadius: 8,
    padding: 12,
    color: "#fff",
    marginBottom: 15,
    fontSize: 16,
  },
  modalButtons: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginTop: 10,
  },
  modalButton: {
    flex: 0.48,
    padding: 14,
    borderRadius: 8,
    alignItems: "center",
  },
  cancelButton: {
    backgroundColor: "#444",
  },
  confirmButton: {
    backgroundColor: Colors.PRIMARY_COLOR,
  },
  buttonText: {
    color: "#fff",
    fontWeight: "bold",
    fontSize: 16,
  },
  dangerItem: {
    borderColor: "#ff444433",
    borderWidth: 1,
    marginTop: 20, // Separar un poco la zona de peligro
  },
  dangerLabel: {
    color: "#ff4444",
  },
  dangerValue: {
    color: "#ff6666",
    fontSize: 14, // Un poco más pequeño para que parezca una advertencia
  },
});

export default ConfigScreen;
