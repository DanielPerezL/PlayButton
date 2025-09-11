import { createNativeStackNavigator } from "@react-navigation/native-stack";
import HeaderMenuButton from "../components/HeaderMenuButton";
import SugerenciasScreen from "../screens/SugerenciasScreen";
import ConfigScreen from "../screens/ConfigScreen";

const Stack = createNativeStackNavigator();

const ConfigurationStack = () => {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: "#333" },
        headerTintColor: "#fff",
      }}
    >
      <Stack.Screen
        name="ConfigScreen"
        component={ConfigScreen}
        options={{
          title: "Configuración",
          headerLeft: () => <HeaderMenuButton />,
        }}
      />
      <Stack.Screen
        name="SugerenciasScreen"
        component={SugerenciasScreen}
        options={{ title: "Sugerir canciones" }}
      />
    </Stack.Navigator>
  );
};

export default ConfigurationStack;
