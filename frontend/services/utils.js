export const formatTime = (seconds) => {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = Math.floor(seconds % 60);
    return `${minutes}:${remainingSeconds < 10 ? "0" : ""}${remainingSeconds}`;
};

export const shuffleArray = (array) => {
    let shuffled = [...array];
    for (let i = shuffled.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
    }
    return shuffled;
};

/*PARA POSIBLE DESCARGAR MUSICA PARA ESCUCHAR SIN CONEXION
  export const cleanCache = async () => {
    //DEV
    //return;
    try {
        // Verifica si estamos en la web
        if (Platform.OS === 'web') {
            return;
        }

        // Para iOS y Android
        const files = await FileSystem.readDirectoryAsync(FileSystem.cacheDirectory);
        for (const file of files) {
            if (file.endsWith('.mp3')) {
                await FileSystem.deleteAsync(FileSystem.cacheDirectory + file);
            }
        }
    } catch (error) {
        console.error("Error al limpiar la caché:", error);
    }
};*/
