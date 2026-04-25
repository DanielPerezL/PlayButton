import {TouchableOpacity, Text, StyleSheet, View} from 'react-native';
import Ionicons from 'react-native-vector-icons/Ionicons';
import {useNavigation} from '@react-navigation/native';
import Colors from '../services/colors';
import {useState} from 'react';
import {togglePlaylistFavorite} from '../services/apiService';

const PlaylistCard = ({playlist}) => {
  const navigation = useNavigation();
  const [isFav, setIsFav] = useState(playlist.is_favorite);
  const [favCount, setFavCount] = useState(playlist.favorites_count);

  const handleToggleFav = async () => {
    const prevFav = isFav;
    const prevCount = favCount;

    setIsFav(!prevFav);
    setFavCount(prevFav ? prevCount - 1 : prevCount + 1);

    const newCount = await togglePlaylistFavorite(playlist.id);
    if (newCount === null) {
      // Si falló, revertimos al estado anterior
      setIsFav(prevFav);
      setFavCount(prevCount);
    } else {
      // Sincronizamos con el conteo real del servidor
      setFavCount(newCount);
      playlist.is_favorite = !prevFav;
      playlist.favorites_count = newCount;
    }
  };

  return (
    <TouchableOpacity
      style={styles.card}
      onPress={() => navigation.navigate('PlaylistDetail', {playlist})}
      activeOpacity={0.7}>
      <View style={styles.infoContainer}>
        <Text style={styles.name} numberOfLines={1}>
          {playlist.name}
        </Text>

        <View style={styles.bottomRow}>
          <View style={styles.row}>
            <Ionicons name="person-circle-outline" size={16} color="#aaa" />
            <Text style={styles.subText}>{playlist.user}</Text>

            <Ionicons
              name="heart"
              size={14}
              color={isFav ? '#ff4444' : '#555'}
              style={{marginLeft: 15}}
            />
            <Text style={styles.subText}>{favCount}</Text>

            {!playlist.is_public && (
              <Text style={[styles.status, styles.private, {marginLeft: 15}]}>
                Privada
              </Text>
            )}
          </View>
        </View>
      </View>

      {/* Botón interactivo de Favorito */}
      <TouchableOpacity
        onPress={handleToggleFav}
        style={styles.favButton}
        hitSlop={{top: 10, bottom: 10, left: 10, right: 10}}>
        <Ionicons
          name={isFav ? 'heart' : 'heart-outline'}
          size={24}
          color={isFav ? '#ff4444' : '#aaa'}
        />
      </TouchableOpacity>

      <Ionicons name="chevron-forward" size={20} color="#555" />
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#1e1e1e',
    marginBottom: 10,
    borderRadius: 12,
    padding: 16,
    flexDirection: 'row',
    alignItems: 'center',
  },
  infoContainer: {flex: 1},
  name: {color: '#fff', fontSize: 18, fontWeight: 'bold', marginBottom: 4},
  bottomRow: {flexDirection: 'row', alignItems: 'center'},
  row: {flexDirection: 'row', alignItems: 'center', gap: 4},
  subText: {color: '#aaa', fontSize: 14},
  status: {
    fontSize: 12,
    fontWeight: 'bold',
    textTransform: 'uppercase',
  },
  public: {color: Colors.PRIMARY_COLOR},
  private: {color: Colors.ERROR_COLOR},
  favButton: {
    paddingHorizontal: 12,
  },
});

export default PlaylistCard;
