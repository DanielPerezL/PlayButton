import { useState } from "react";
import {
  Text,
  TouchableOpacity,
  StyleSheet,
  FlatList,
  View,
} from "react-native";
import { useNavigation } from "@react-navigation/native";
import { Ionicons } from "@expo/vector-icons";
import Colors from "../services/colors";

const PlaylistGrid = ({ playlists, showUser, onRefreshPlaylists }) => {
  const navigation = useNavigation();
  const [refreshing, setRefreshing] = useState(false);

  const handleRefresh = async () => {
    setRefreshing(true);
    await onRefreshPlaylists(true);
    setRefreshing(false);
  };

  const renderPlaylistItem = ({ item }) => (
    <TouchableOpacity
      style={styles.playlistItem}
      onPress={() => navigation.navigate("PlaylistDetail", { playlist: item })}
      activeOpacity={0.8} // Añade un efecto de opacidad al presionar el item
    >
      <Text style={styles.playlistName}>{item.name}</Text>
      {showUser && (
        <View style={styles.userContainer}>
          <Ionicons name="person-circle" size={18} color="#aaa" />
          <Text style={styles.userName}>{item.user}</Text>
        </View>
      )}
      {!showUser && (
        <Text
          style={[
            styles.isPublic,
            item.is_public ? styles.public : styles.private,
          ]}
        >
          {item.is_public ? "Pública" : "Privada"}
        </Text>
      )}
    </TouchableOpacity>
  );

  // Si no hay playlists, mostramos un mensaje
  if (playlists.length === 0) {
    return (
      <View style={styles.emptyContainer}>
        <Text style={styles.emptyText}>No hay playlists disponibles</Text>
      </View>
    );
  }

  return (
    <FlatList
      data={playlists}
      renderItem={renderPlaylistItem}
      keyExtractor={(item) => item.id.toString()}
      numColumns={1}
      contentContainerStyle={styles.contentContainer}
      refreshing={onRefreshPlaylists && refreshing}
      onRefresh={onRefreshPlaylists && handleRefresh}
    />
  );
};

const styles = StyleSheet.create({
  playlistItem: {
    backgroundColor: "#444", // Un tono más suave para el fondo
    margin: 10,
    borderRadius: 12, // Bordes más suaves
    padding: 15,
    flex: 1,
    alignItems: "center",
    height: 140, // Aumenté la altura para que haya más espacio
    justifyContent: "center",
    shadowColor: "#000", // Añadimos sombra
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 5,
    elevation: 5, // Sombra para Android
  },
  playlistName: {
    color: "white",
    fontSize: 22,
    fontWeight: "bold",
    textAlign: "center",
    marginBottom: 5,
  },
  userContainer: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 5,
  },
  userName: {
    color: "#ccc", // Color más sutil para el nombre del usuario
    fontSize: 14,
    marginLeft: 5,
  },
  isPublic: {
    marginTop: 10, // Más espacio entre el texto y el estado de visibilidad
    fontSize: 16,
    fontWeight: "bold",
  },
  public: {
    color: Colors.PRIMARY_PASTEL_COLOR,
  },
  private: {
    color: Colors.ERROR_COLOR,
  },
  contentContainer: {
    paddingBottom: 20, // Espacio adicional al final del contenido
  },
  emptyContainer: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    padding: 20,
  },
  emptyText: {
    color: "#bbb",
    fontSize: 18,
    fontStyle: "italic",
  },
});

export default PlaylistGrid;
