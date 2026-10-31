import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";

const ENTRADAS_FIJAS = new Set(["trabajador-servicio", "inspector-documento"]);

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
        emergente: path.resolve(__dirname, "src/paginas/emergente/index.html"),
        bloqueado: path.resolve(__dirname, "src/paginas/bloqueado/index.html"),
        opciones: path.resolve(__dirname, "src/paginas/opciones/index.html"),
        "trabajador-servicio": path.resolve(__dirname, "src/segundo-plano/trabajador-servicio.ts"),
        "inspector-documento": path.resolve(__dirname, "src/contenido/inspector-documento.ts"),
      },
      output: {
        entryFileNames: (fragmento) =>
          ENTRADAS_FIJAS.has(fragmento.name)
            ? `${fragmento.name}.js`
            : "assets/[name]-[hash].js",
        chunkFileNames: "assets/[name]-[hash].js",
        assetFileNames: "assets/[name]-[hash].[ext]",
      },
    },
  },
});
