import path from "node:path";

import { lingui, linguiTransformerBabelPreset } from "@lingui/vite-plugin";
import babel from "@rolldown/plugin-babel";
import tailwindcss from "@tailwindcss/vite";
import { tanstackRouter } from "@tanstack/router-plugin/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

const linguiConfig = {
  configPath: path.resolve(import.meta.dirname, "./lingui.config.ts"),
};

export default defineConfig({
  plugins: [
    tanstackRouter({ autoCodeSplitting: true, target: "react" }),
    react(),
    lingui(linguiConfig),
    babel({
      presets: [linguiTransformerBabelPreset(undefined, linguiConfig)],
    }),
    tailwindcss(),
  ],
  resolve: {
    alias: {
      "@": path.resolve(import.meta.dirname, "./src"),
    },
  },
});
