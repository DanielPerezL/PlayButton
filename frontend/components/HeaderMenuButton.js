import { TouchableOpacity } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";

const HeaderMenuButton = ({ style }) => {
  const navigation = useNavigation();

  return (
    <TouchableOpacity
      onPress={() => navigation.openDrawer()}
      style={[{ marginRight: 10 }, style]} // Combina el estilo predeterminado con el estilo pasado por prop
    >
      <Ionicons name="menu" size={26} color="#fff" />
    </TouchableOpacity>
  );
};

export default HeaderMenuButton;
