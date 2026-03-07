import React, { useCallback, useEffect, useState } from "react";
import { View, Text, StyleSheet } from "react-native";
import { useNavigation, useRoute } from "@react-navigation/native";
import NewPlaylistButton from "../components/NewPlaylistButton";
import PlaylistList from "../components/PlaylistList";
import { fetchUserPlaylists, getLoggedUserId } from "../services/apiService";

const UserPlaylistsScreen = () => {
  const navigation = useNavigation();
  const route = useRoute();

  // Sacamos el objeto user de los parámetros (si existe)
  const userParam = route.params?.user;

  const [targetUser, setTargetUser] = useState(null);
  const [isOwnProfile, setIsOwnProfile] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const setupUser = async () => {
      const loggedUserId = await getLoggedUserId();

      if (userParam && userParam.id !== loggedUserId) {
        // Estamos viendo el perfil de OTRO
        setTargetUser(userParam);
        setIsOwnProfile(false);
      } else {
        // Estamos viendo NUESTRO perfil
        setTargetUser({ id: loggedUserId, name: "Mis Playlists" });
        setIsOwnProfile(true);
      }
      setLoading(false);
    };

    setupUser();
  }, [userParam]);

  // Esta función se la pasamos a PlaylistList
  // Usamos el ID de targetUser que ya decidimos en el useEffect
  const fetchPlaylists = useCallback(
    async (offset, limit, search) => {
      if (!targetUser?.id) return { playlists: [], has_more: false };
      return await fetchUserPlaylists(targetUser.id, offset, limit, search);
    },
    [targetUser],
  );

  if (loading || !targetUser) return null;

  return (
    <View style={styles.container}>
      <Text style={styles.title}>
        {isOwnProfile ? "Mis Playlists" : `Playlists de ${targetUser.name}`}
      </Text>

      {/* El key={targetUser.id} fuerza a PlaylistList a reiniciarse si cambias de perfil */}
      <PlaylistList
        key={targetUser.id}
        fetchFunction={fetchPlaylists}
        publicCard={!isOwnProfile}
      />

      {/* Solo mostramos el botón de añadir si es MI perfil */}
      {isOwnProfile && (
        <NewPlaylistButton
          onPress={() => navigation.navigate("NewPlaylistScreen")}
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#121212",
    paddingTop: 10,
  },
  title: {
    fontSize: 24,
    color: "#fff",
    fontWeight: "bold",
    textAlign: "center",
    marginVertical: 15,
  },
});

export default UserPlaylistsScreen;
