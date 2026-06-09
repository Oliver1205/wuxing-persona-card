import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

const backendProxy = {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
  '/s': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
};

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: backendProxy,
  },
  preview: {
    port: 4173,
    proxy: backendProxy,
  },
});
