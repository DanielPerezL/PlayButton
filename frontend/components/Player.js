import { useState, useEffect, useRef } from "react";
import {
  View,
  Text,
  TouchableOpacity,
  FlatList,
  ActivityIndicator,
} from "react-native";
import Slider from "@react-native-community/slider";
import TrackPlayer, {
  State,
  Capability,
  useProgress,
  usePlaybackState,
  Event,
  AppKilledPlaybackBehavior,
} from "react-native-track-player";
import { StyleSheet } from "react-native";
import { FontAwesome } from "@expo/vector-icons";
import { formatTime } from "../services/utils";
import { getSignedSongUrl, PRODUCTION } from "../services/apiService";
import Colors from "../services/colors";
import { play } from "react-native-track-player/lib/src/trackPlayer";

const Player = ({ songs, onSongsEnd }) => {
  const [loading, setLoading] = useState(false);
  const [currentSongIndex, setCurrentSongIndex] = useState(0);
  const [currentSong, setCurrentSong] = useState(null);
  const [isQueueVisible, setIsQueueVisible] = useState(false);
  const playbackState = usePlaybackState();
  const progress = useProgress();
  const queueRef = useRef(null);

  useEffect(() => {
    const setupPlayer = async () => {
      await TrackPlayer.setupPlayer();
      TrackPlayer.updateOptions({
        android: {
          appKilledPlaybackBehavior:
            AppKilledPlaybackBehavior.StopPlaybackAndRemoveNotification,
        },
        capabilities: [
          Capability.Play,
          Capability.Pause,
          Capability.SkipToNext,
          Capability.SkipToPrevious,
        ],
      });

      const onPlay = TrackPlayer.addEventListener(
        Event.RemotePlay,
        async () => {
          await TrackPlayer.play();
        }
      );

      const onPause = TrackPlayer.addEventListener(
        Event.RemotePause,
        async () => {
          await TrackPlayer.pause();
        }
      );

      const onTrackEnded = TrackPlayer.addEventListener(
        Event.PlaybackQueueEnded,
        async () => {
          playNext();
        }
      );

      return () => {
        onPlay.remove();
        onPause.remove();
        onTrackEnded.remove();
      };
    };

    setupPlayer();
  }, []);

  useEffect(() => {
    const onNext = TrackPlayer.addEventListener(Event.RemoteNext, () => {
      setCurrentSongIndex((prev) => prev + 1);
    });

    const onPrevious = TrackPlayer.addEventListener(
      Event.RemotePrevious,
      () => {
        setCurrentSongIndex((prev) => prev - 1);
      }
    );

    const onPlaybackError = TrackPlayer.addEventListener(
      Event.PlaybackError,
      async () => {
        const progress = await TrackPlayer.getProgress();
        const currentPosition = progress.position || 0;

        try {
          const trackId = await TrackPlayer.getTrack(0).then(
            (track) => track?.id
          );
          const songIndex = songs.findIndex(
            (s) => s.id.toString() === trackId.toString()
          );
          playSong(songIndex, currentPosition);
        } catch (error) {
          console.error("Error al obtener la pista actual:", error);
          return;
        }
      }
    );

    return () => {
      onNext.remove();
      onPrevious.remove();
      onPlaybackError.remove();
    };
  }, [songs]);

  useEffect(() => {
    const prepareTrackPlayer = async () => {
      await TrackPlayer.stop();
    };
    prepareTrackPlayer();
    setCurrentSongIndex(0);
    if (songs && songs.length > 0) playMusic();
  }, [songs]);

  const songsEnded = async () => {
    await TrackPlayer.stop();
    await TrackPlayer.removeUpcomingTracks();
    onSongsEnd();
  };

  const playSong = async (songIndex, position = 0) => {
    setLoading(true);

    console.log(
      `Reproduciendo canción en el índice: ${songIndex}, posición: ${position}`
    );

    const song = songs[songIndex];
    if (!song) {
      setLoading(false);
      return songsEnded();
    }

    console.log(
      `Reproduciendo canción: ${song.name} (ID: ${song.id}, posición: ${position})`
    );

    const signedUrl = await getSignedSongUrl(song.id);
    if (!signedUrl) {
      console.error(`No se pudo obtener la URL para la canción ID ${song.id}`);
      setLoading(false);
      return;
    }

    let artist = null;
    let title = null;
    if (song.name && song.name.includes(" - ")) {
      [artist, title] = song.name.split(" - ");
    }

    const track = {
      id: song.id.toString(),
      url: signedUrl,
      title: title || song.name || "Sin título",
      artist: artist || "Desconocido",
    };

    try {
      await TrackPlayer.reset(); // borramos todo
      await TrackPlayer.add([track]); // solo la canción actual
      if (position > 0) {
        await TrackPlayer.seekTo(position);
      }
      await TrackPlayer.play();
      setCurrentSong(song);
    } catch (error) {
      console.error("Error al cambiar de pista:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const handleTrackChange = async () => {
      playSong(currentSongIndex);
    };

    if (currentSongIndex < 0) {
      setCurrentSongIndex(0);
      return;
    }
    handleTrackChange();
  }, [currentSongIndex]);

  const playMusic = async () => {
    if (songs && songs.length > 0 && currentSongIndex !== null) playSong(0);
  };

  const togglePlayPause = async () => {
    if (playbackState.state === State.Playing) {
      await TrackPlayer.pause();
    } else {
      await TrackPlayer.play();
    }
  };

  const playNext = () => setCurrentSongIndex((prev) => prev + 1);
  const playPrev = () => setCurrentSongIndex((prev) => prev - 1);
  const seek = async (value) => await TrackPlayer.seekTo(value);

  const isLoading =
    (playbackState.state !== State.Playing &&
      playbackState.state !== State.Paused &&
      playbackState.state !== State.Ready &&
      loading) ||
    playbackState.state === State.Buffering;

  return (
    <View style={styles.container}>
      {/* Título */}
      {isLoading ? (
        <Text style={styles.songTitle}>
          {currentSong?.name || "Cargando..."}
        </Text>
      ) : (
        <Text style={styles.songTitle}>
          {currentSong?.name || "No hay canción en reproducción."}
        </Text>
      )}
      {/* Slider */}
      <View style={styles.sliderContainer}>
        <Text style={styles.time}>{formatTime(progress.position)}</Text>
        <Slider
          style={styles.slider}
          minimumValue={0}
          maximumValue={progress.duration || 1}
          value={progress.position}
          minimumTrackTintColor={Colors.PRIMARY_COLOR}
          maximumTrackTintColor="#555"
          thumbTintColor={Colors.PRIMARY_COLOR}
          onSlidingComplete={seek}
        />
        <Text style={styles.time}>{formatTime(progress.duration)}</Text>
      </View>
      {/* Controles */}
      <View style={styles.controlsContainer}>
        <TouchableOpacity onPress={playPrev}>
          <FontAwesome name="step-backward" size={28} color="#fff" />
        </TouchableOpacity>
        <TouchableOpacity
          onPress={togglePlayPause}
          style={styles.playPauseButton}
        >
          <TouchableOpacity
            onPress={togglePlayPause}
            style={styles.playPauseButton}
          >
            {isLoading ? (
              <View style={styles.playPauseButton}>
                <ActivityIndicator size="large" color="#fff" />
              </View>
            ) : (
              <FontAwesome
                name={playbackState.state === State.Playing ? "pause" : "play"}
                size={30}
                color="white"
                style={
                  playbackState.state !== State.Playing
                    ? { marginLeft: 4 } // corrige el centrado óptico
                    : {}
                }
              />
            )}
          </TouchableOpacity>
        </TouchableOpacity>
        <TouchableOpacity onPress={playNext}>
          <FontAwesome name="step-forward" size={28} color="#fff" />
        </TouchableOpacity>
      </View>
      {!PRODUCTION && (
        <Text style={{ color: Colors.PRIMARY_PASTEL_COLOR }}>
          Estado: {playbackState.state}
        </Text>
      )}
      {/* Botón para mostrar/ocultar cola */}
      <TouchableOpacity onPress={() => setIsQueueVisible(!isQueueVisible)}>
        <Text style={styles.toggleQueueText}>
          {isQueueVisible ? "Ocultar cola" : "Mostrar cola"}
        </Text>
      </TouchableOpacity>
      {/* Lista de canciones */}
      {isQueueVisible && (
        <FlatList
          style={styles.queue}
          ref={queueRef}
          data={songs.slice(currentSongIndex)}
          keyExtractor={(item) => item.id}
          renderItem={({ item, index }) => (
            <TouchableOpacity
              style={[styles.queueItem, index === 0 && styles.activeItem]}
              onPress={() => {
                setCurrentSongIndex(currentSongIndex + index);
                setTimeout(() => {
                  queueRef.current?.scrollToOffset({
                    offset: 0,
                    animated: true,
                  });
                }, 100);
              }}
              activeOpacity={0.7} // efecto de opacidad al tocar
            >
              <Text style={styles.queueItemText}>{item.name}</Text>
            </TouchableOpacity>
          )}
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center", // Alinea verticalmente al centro
    alignItems: "center",
    padding: 20,
  },
  songTitle: {
    color: "#fff",
    fontSize: 18,
    fontWeight: "600",
    textAlign: "center",
    marginBottom: 20,
  },
  sliderContainer: {
    flexDirection: "row",
    alignItems: "center",
    width: "100%",
    marginBottom: 20,
  },
  slider: {
    flex: 1,
    marginHorizontal: 10,
  },
  time: {
    color: "#aaa",
    fontSize: 12,
    width: 40,
    textAlign: "center",
  },
  controlsContainer: {
    flexDirection: "row",
    justifyContent: "space-around",
    alignItems: "center",
    width: "60%",
    marginBottom: 20,
  },
  playPauseButton: {
    backgroundColor: Colors.PRIMARY_COLOR,
    padding: 16,
    borderRadius: 50,
    alignItems: "center",
    justifyContent: "center",
    height: 70,
    width: 70,
  },
  toggleQueueText: {
    color: Colors.PRIMARY_COLOR,
    fontWeight: "bold",
    fontSize: 14,
    marginBottom: 10,
  },
  queue: {
    padding: 10,
    backgroundColor: "#222",
    borderRadius: 20,
    width: 330,
  },
  queueItem: {
    backgroundColor: "#333",
    padding: 10,
    borderRadius: 10,
    marginBottom: 10,
    alignItems: "center",
  },
  activeItem: {
    backgroundColor: Colors.PRIMARY_COLOR,
    color: "#000",
  },
  queueItemText: {
    fontSize: 16,
    color: "#fff",
  },
});

export default Player;
