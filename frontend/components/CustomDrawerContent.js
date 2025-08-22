import {
  DrawerContentScrollView,
  DrawerItemList,
} from "@react-navigation/drawer";
import { View, TouchableOpacity, Text } from "react-native";
import Ionicons from "react-native-vector-icons/Ionicons";
import { useAlert } from "../services/alertContext";

export const CustomDrawerContent = (props) => {
  const { showAlert } = useAlert();

  // Función para manejar el cierre de sesión
  const handleLogout = () => {
    showAlert("Cerrar sesión", "¿Estás seguro de que quieres cerrar sesión?", [
      { text: "Cancelar", style: "cancel" },
      {
        text: "Confirmar",
        style: "destructive",
        onPress: props.onLogout,
      },
    ]);
  };

  return (
    <View style={{ flex: 1 }}>
      <DrawerContentScrollView {...props}>
        <DrawerItemList {...props} />
      </DrawerContentScrollView>

      {/* Contenedor inferior con Config y Logout */}
      <View
        style={{
          flexDirection: "row",
          position: "absolute",
          bottom: 20,
          left: 15,
          right: 15,
          padding: 5,
        }}
      >
        {/* Botón de cerrar sesión */}
        <TouchableOpacity
          style={{
            backgroundColor: "#ff4d4d",
            padding: 15,
            borderRadius: 6,
            flex: 1,
            alignItems: "center",
            justifyContent: "center",
            marginRight: 20,
          }}
          onPress={handleLogout}
        >
          <Text style={{ color: "#fff", fontWeight: "bold" }}>
            Cerrar sesión
          </Text>
        </TouchableOpacity>

        {/* Botón de configuración */}
        <TouchableOpacity
          style={{
            backgroundColor: "#444",
            padding: 15,
            borderRadius: 6,
            flex: 0.25,
            alignItems: "center",
            justifyContent: "center",
          }}
          onPress={() => props.navigation.navigate("Configuración")}
        >
          <Ionicons name="settings-outline" size={20} color="#fff" />
        </TouchableOpacity>
      </View>
    </View>
  );
};

export default CustomDrawerContent;
