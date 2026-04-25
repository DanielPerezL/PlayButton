const {getDefaultConfig, mergeConfig} = require('@react-native/metro-config');

/**
 * Metro configuration
 * https://reactnative.dev/docs/metro
 *
 * @type {import('@react-native/metro-config').MetroConfig}
 */
const config = {
  watchFolders: [],
  resolver: {
    blockList: [/node_modules\/react-native-reanimated\/android\/\.cxx\/.*/],
  },
};

module.exports = mergeConfig(getDefaultConfig(__dirname), config);
