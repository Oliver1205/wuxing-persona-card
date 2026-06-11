import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

const backendTarget = process.env.BACKEND_PROXY_TARGET || 'http://localhost:8080';

const backendProxy = {
  '/api': {
    target: backendTarget,
    changeOrigin: true,
  },
  '^/s/': {
    target: backendTarget,
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
