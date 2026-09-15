import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../backend/static',
  },
  css: {
    preprocessorOptions: {
      scss: {
        // Bootstrap 5.3 se distribuye con @import, deprecado en Dart Sass >=1.80.
        // No se puede migrar a @use hasta Bootstrap 6: sus variables llevan
        // !default dentro de un parcial cargado por @import, así que no son
        // configurables como módulo. quietDeps silencia lo que viene de
        // node_modules; silenceDeprecations, el @import que escribimos nosotros
        // en styles/main.scss.
        quietDeps: true,
        // quietDeps solo silencia lo que nace bajo node_modules; el @import de
        // Bootstrap lo escribimos nosotros en styles/main.scss, así que hay que
        // nombrarlo aquí. La lista está recortada a lo que Sass emite de verdad:
        // si aparece un aviso nuevo, es nuestro y queremos verlo.
        silenceDeprecations: ['import'],
      },
    },
  },
})
