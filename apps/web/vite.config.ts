import path from "node:path";

import { lingui, linguiTransformerBabelPreset } from "@lingui/vite-plugin";
import babel from "@rolldown/plugin-babel";
import tailwindcss from "@tailwindcss/vite";
import { tanstackRouter } from "@tanstack/router-plugin/vite";
import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";

const linguiConfig = {
  configPath: path.resolve(import.meta.dirname, "./lingui.config.ts"),
};
const repositoryRoot = path.resolve(import.meta.dirname, "../..");

export default defineConfig(({ mode }) => {
  const loadedEnv = loadEnv(mode, repositoryRoot, "");
  const apiMockingValue =
    process.env.VITE_API_MOCKING ?? loadedEnv.VITE_API_MOCKING;
  const apiUrlValue = process.env.VITE_API_URL ?? loadedEnv.VITE_API_URL;
  const apiUrl = apiUrlValue ? new URL(apiUrlValue) : undefined;

  if (apiUrl && apiUrl.pathname.replace(/\/$/, "") !== "/api/v1") {
    throw new Error("VITE_API_URL must target the /api/v1 service base path.");
  }

  return {
    envDir: repositoryRoot,
    plugins: [
      {
        name: "require-development-api-source",
        configureServer() {
          if (apiMockingValue !== "true" && !apiUrl) {
            throw new Error(
              "Web development requires VITE_API_MOCKING=true or a configured VITE_API_URL.",
            );
          }
        },
      },
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
    server: apiUrl
      ? {
          proxy: {
            "/api": {
              changeOrigin: true,
              target: apiUrl.origin,
            },
          },
        }
      : undefined,
  };
});
