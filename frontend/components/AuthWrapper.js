import { useEffect } from "react";
import { View } from "react-native";
import AccessMenu from "../components/AccessMenu";
import { isLoggedIn } from "../services/apiService";

const AuthWrapper = ({ children, onLoginSuccess, logged }) => {
  const checkLogin = async () => {
    const result = await isLoggedIn();
    setLoggedIn(result);
  };

  useEffect(() => {
    checkLogin();
  }, []);

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
