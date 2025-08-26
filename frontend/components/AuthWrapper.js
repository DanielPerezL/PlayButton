import { View } from "react-native";
import AccessMenu from "../components/AccessMenu";

const AuthWrapper = ({ children, onLoginSuccess, logged }) => {
  if (!logged) {
    return (
      <View style={{ flex: 1, backgroundColor: "#222" }}>
        <AccessMenu onLoginSuccess={onLoginSuccess} />
      </View>
    );
  }

  return <View style={{ flex: 1 }}>{children}</View>;
};

export default AuthWrapper;
