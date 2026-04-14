import path from 'node:path'
import tailwindcss from '@tailwindcss/vite'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

/** 与 Halo 插件静态资源路径一致（见 templates/dishes.html、ReverseProxy） */
const PLUGIN_ASSET_BASE = '/plugins/dishes/assets/dishes-frontend/'

export default defineConfig({
  base: PLUGIN_ASSET_BASE,
  publicDir: 'public',
  build: {
    outDir: path.resolve(__dirname, 'build/dist'),
    emptyOutDir: true,
    rollupOptions: {
      output: {
        entryFileNames: 'app.js',
        chunkFileNames: 'chunks/[name].js',
        assetFileNames: 'assets/[name][extname]',
      },
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  plugins: [vue(), tailwindcss()],
  server: {
    host: true,
    port: 5173,
    proxy: {
      '/apis/plugins/dishes': {
        target: 'http://127.0.0.1:8090',
        changeOrigin: true,
      },
    },
  },
})
