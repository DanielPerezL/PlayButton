import { useEffect, useState } from "react";
import { View, Text, StyleSheet, TouchableOpacity } from "react-native";
import Player from "../components/Player";
import { useRoute } from "@react-navigation/native";
import { fetchSongsData, isLoggedIn } from "../services/apiService";
import { shuffleArray } from "../services/utils";
import Colors from "../services/colors";

const PlayerScreen = () => {
  const [refresh, setRefresh] = useState(false);

  const route = useRoute();
  const songsOnRoute = route.params?.songs;
  const playlistNameOnRoute = route.params?.playlist_name;

  const [songs, setSongs] = useState([]);
  const [isGeneralMode, setIsGeneralMode] = useState(false);
  const limit = 10; //Aleatorias de 10 en 10

  const loadGeneralSongs = async () => {
    const generalSongs = await fetchSongsData(limit);
    setSongs(shuffleArray(generalSongs));
    setIsGeneralMode(true); // Switch to general mode
    setRefresh((p) => !p);
  };

  const loadPlaylistSongs = () => {
    if (songsOnRoute) {
      setSongs(shuffleArray(songsOnRoute));
      setIsGeneralMode(false); // Switch to playlist mode
      setRefresh((p) => !p);
    }
  };

  const handleSongsEnd = async () => {
    if (isGeneralMode) {
      await loadGeneralSongs();
    } else {
      setSongs((prevSongs) => shuffleArray(prevSongs));
    }
  };

  useEffect(() => {
    const initSongs = async () => {
      if (!songsOnRoute && (await isLoggedIn())) {
        loadGeneralSongs();
      } else if (songsOnRoute) {
        loadPlaylistSongs();
      }
    };
    initSongs();
  }, [songsOnRoute]);

  return (
    <View style={styles.container}>
      {/* Display playlist name if in playlist mode */}
      {!isGeneralMode && playlistNameOnRoute && (
        <Text style={styles.playlistTitle}>{playlistNameOnRoute}</Text>
      )}
      {isGeneralMode && <Text style={styles.playlistTitle}>Modo Zen</Text>}

      {/* The player */}
      <Player key={refresh} songs={songs} onSongsEnd={handleSongsEnd} />

      {/* Button to switch to general songs */}
      {!isGeneralMode && (
        <TouchableOpacity style={styles.resetButton} onPress={loadGeneralSongs}>
          <Text style={styles.resetButtonText}>Modo Zen</Text>
        </TouchableOpacity>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#121212",
    padding: 16,
  },
  playlistTitle: {
    fontSize: 20,
    fontWeight: "bold",
    color: "#fff",
    textAlign: "center",
    marginTop: 20,
  },
  resetButton: {
    backgroundColor: Colors.PRIMARY_COLOR,
    padding: 12,
    borderRadius: 8,
    marginTop: 20,
    alignItems: "center",
  },
  resetButtonText: {
    color: "#fff",
    fontSize: 16,
    fontWeight: "bold",
  },
});

export default PlayerScreen;
