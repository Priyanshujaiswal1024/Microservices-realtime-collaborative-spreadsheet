import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  define: {
    global: 'window',
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8083',
        ws: true,
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('error', (err) => {
            // Ignore normal socket disconnects on tab refresh
          });
          proxy.on('proxyReqWsError', (err) => {
            // Ignore normal ws socket resets/disconnects on refresh
          });
        }
      }
    }
  }
});
