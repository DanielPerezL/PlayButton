module.exports = function (api) {
    api.cache(true);
    return {
      presets: ['babel-preset-expo'],
      plugins: [
        [
          'inline-dotenv',
          {
            path: '.env', // Ruta al archivo .env
          },
        ],
      ],
    };
  };