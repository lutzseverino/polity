import path from "node:path";

import { lingui, linguiTransformerBabelPreset } from "@lingui/vite-plugin";
import babel from "@rolldown/plugin-babel";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

const linguiConfig = {
  configPath: path.resolve(import.meta.dirname, "./lingui.config.ts"),
};

export default defineConfig({
  plugins: [
    react(),
    lingui(linguiConfig),
    babel({
      presets: [linguiTransformerBabelPreset(undefined, linguiConfig)],
    }),
  ],
  resolve: {
    alias: {
      "@": path.resolve(import.meta.dirname, "./src"),
    },
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
  },
});
