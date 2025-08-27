import { useEffect, useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  Linking,
} from "react-native";
import Constants from "expo-constants";
import Colors from "../services/colors";
import { getApiBaseUrl } from "../services/apiService";
import { useAlert } from "../services/alertContext";

const ConfigScreen = () => {
  const { showAlert } = useAlert();
  const [apiUrl, setApiUrl] = useState("");

  useEffect(() => {
    const fetchApiUrl = async () => {
      const url = await getApiBaseUrl();
      setApiUrl(url);
    };

    fetchApiUrl();
  }, []);

  const onPressItem = (item) => {
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

    return (
      <TouchableOpacity
        onPress={item.msg ? () => onPressItem(item) : undefined}
        style={styles.item}
        activeOpacity={item.msg ? 0.6 : 1}
      >
        <Text style={styles.label}>{item.key}</Text>
        <Text style={styles.value}>{displayValue}</Text>
      </TouchableOpacity>
    );
  };
  // Datos de configuración que se mostrarán en la pantalla
  // "key" debe ser un identificador único para cada elemento
  const configItems = [
    //Servidor actual
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

    //Repo de GitHub
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

    //Versión de app
    { key: "Versión de la app", value: Constants.expoConfig.version },
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
});

export default ConfigScreen;
