import { Modal, View, Text, StyleSheet, TouchableOpacity } from "react-native";
import Colors from "../services/colors";

const CustomAlertModal = ({
  visible,
  title,
  message,
  buttons,
  onRequestClose = () => {},
}) => {
  return (
    <Modal
      visible={visible}
      transparent={true}
      animationType="fade"
      onRequestClose={onRequestClose}
    >
      <View style={styles.overlay}>
        <View style={styles.modalContent}>
          <Text style={styles.title}>{title}</Text>
          <Text style={styles.message}>{message}</Text>

          <View style={styles.buttonsContainer}>
            {buttons.map(({ text, onPress, style }, idx) => (
              <TouchableOpacity
                key={idx}
                style={[
                  styles.button,
                  style === "destructive" && styles.destructiveButton,
                  style === "cancel" && styles.cancelButton,
                ]}
                onPress={() => {
                  onRequestClose();
                  onPress && onPress();
                }}
              >
                <Text
                  style={[
                    styles.buttonText,
                    style === "destructive" && styles.destructiveButtonText,
                    style === "cancel" && styles.cancelButtonText,
                  ]}
                >
                  {text}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: "rgba(0,0,0,0.7)",
    justifyContent: "center",
    alignItems: "center",
    padding: 20,
  },
  modalContent: {
    backgroundColor: "#2a2a2a",
    borderRadius: 14,
    padding: 20,
    width: "100%",
    maxWidth: 400,
  },
  title: {
    fontSize: 20,
    fontWeight: "bold",
    color: "white",
    marginBottom: 10,
  },
  message: {
    fontSize: 16,
    color: "#ddd",
    marginBottom: 20,
  },
  buttonsContainer: {
    flexDirection: "row",
    justifyContent: "flex-end",
    gap: 10,
  },
  button: {
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderRadius: 10,
    backgroundColor: Colors.PRIMARY_COLOR,
    marginLeft: 10,
  },
  destructiveButton: {
    backgroundColor: "#dc3545",
  },
  cancelButton: {
    backgroundColor: "#555",
  },
  buttonText: {
    color: "white",
    fontWeight: "bold",
    fontSize: 16,
  },
  destructiveButtonText: {
    color: "white",
  },
  cancelButtonText: {
    color: "white",
  },
});

export default CustomAlertModal;
