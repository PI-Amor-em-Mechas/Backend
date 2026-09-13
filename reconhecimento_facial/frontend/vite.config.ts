import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const backendProxy = {
  target: "http://127.0.0.1:5000",
  changeOrigin: true,
};

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/me": backendProxy,
      "/set-profile": backendProxy,
      "/logout": backendProxy,
      "/recognize-frame": backendProxy,
      "/confirm": backendProxy,
      "/employees": backendProxy,
      "/register-person": backendProxy,
      "/train-model": backendProxy,
      "/voice-biometry": backendProxy,
      "/voice-phrases": backendProxy,
      "/lgpd": backendProxy,
      "/tts": backendProxy,
      "/socket.io": {
        ...backendProxy,
        ws: true,
      },
    },
  },
});
