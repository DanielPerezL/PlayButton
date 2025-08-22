import { useEffect, useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
} from "react-native";
import Constants from "expo-constants";
import Colors from "../services/colors";
import { getApiBaseUrl } from "../services/apiService";
import { useAlert } from "../services/alertContext";

const ConfigScreen = () => {
  const { showAlert } = useAlert();

  const [apiUrl, setApiUrl] = useState("");

  useEffect(() => {
    (async () => {
      const url = await getApiBaseUrl();
      setApiUrl(url);
    })();
  }, []);

  const onPressItem = (item) => {
    if (item.msg) {
      showAlert(item.msg.title, item.msg.body);
    }
  };

  const renderConfigItem = ({ item }) => (
    <TouchableOpacity
      onPress={() => onPressItem(item)}
      style={styles.item}
      activeOpacity={item.msg ? 0.6 : 1}
    >
      <Text style={styles.label}>{item.key}</Text>
      <Text style={styles.value}>{item.value}</Text>
    </TouchableOpacity>
  );

  // Datos de configuración que se mostrarán en la pantalla
  // "key" debe ser un identificador único para cada elemento
  const configItems = [
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
