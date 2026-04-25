import React, {useRef} from 'react';
import {TouchableOpacity, StyleSheet, Animated, View} from 'react-native';
import Ionicons from 'react-native-vector-icons/Ionicons';
import Colors from '../services/colors';

const NewPlaylistButton = ({
  onPress,
  iconName = 'add',
  backgroundColor = Colors.PRIMARY_COLOR,
}) => {
  // Animación de escala para feedback táctil profesional
  const scaleValue = useRef(new Animated.Value(1)).current;

  const handlePressIn = () => {
    Animated.spring(scaleValue, {
      toValue: 0.9,
      useNativeDriver: true,
      speed: 20,
    }).start();
  };

  const handlePressOut = () => {
    Animated.spring(scaleValue, {
      toValue: 1,
      friction: 3,
      tension: 40,
      useNativeDriver: true,
    }).start();
  };

  return (
    <Animated.View
      style={[styles.container, {transform: [{scale: scaleValue}]}]}>
      <TouchableOpacity
        style={[styles.button, {backgroundColor}]}
        onPress={onPress}
        onPressIn={handlePressIn}
        onPressOut={handlePressOut}
        activeOpacity={1} // Desactivamos la opacidad nativa para usar nuestra escala
      >
        <Ionicons name={iconName} size={32} color="#fff" />
      </TouchableOpacity>
    </Animated.View>
  );
};

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    right: 20,
    bottom: 30,
    // Elevación y sombras
    elevation: 8,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 4},
    shadowOpacity: 0.3,
    shadowRadius: 4.65,
    zIndex: 1000,
  },
  button: {
    width: 60,
    height: 60,
    borderRadius: 30,
    alignItems: 'center',
    justifyContent: 'center',
  },
});

export default NewPlaylistButton;
