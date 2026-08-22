import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  build: {
    outDir: "dist",
    emptyOutDir: true,
    rollupOptions: {
      input: {
        popup: path.resolve(__dirname, "src/pages/popup/index.html"),
        bloqueado: path.resolve(__dirname, "src/pages/bloqueado/index.html"),
        opciones: path.resolve(__dirname, "src/pages/opciones/index.html"),
        "service-worker": path.resolve(__dirname, "src/background/service-worker.ts"),
        "content-script": path.resolve(__dirname, "src/content/content-script.ts"),
      },
      output: {
        entryFileNames: (chunkInfo) => {
          if (chunkInfo.name === "service-worker") {
            return "service-worker.js";
          }
          if (chunkInfo.name === "content-script") {
            return "content-script.js";
          }
          return "assets/[name]-[hash].js";
        },
        chunkFileNames: "assets/[name]-[hash].js",
        assetFileNames: "assets/[name]-[hash].[ext]",
      },
    },
  },
});
