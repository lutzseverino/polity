import { rm } from "node:fs/promises";
import path from "node:path";

import { lingui, linguiTransformerBabelPreset } from "@lingui/vite-plugin";
import babel from "@rolldown/plugin-babel";
import tailwindcss from "@tailwindcss/vite";
import { tanstackRouter } from "@tanstack/router-plugin/vite";
import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";

import { resolveDevelopmentApiSource } from "./src/mocks/development-api-source";

const linguiConfig = {
  configPath: path.resolve(import.meta.dirname, "./lingui.config.ts"),
};
const repositoryRoot = path.resolve(import.meta.dirname, "../..");

export default defineConfig(({ mode }) => {
  const loadedEnv = loadEnv(mode, repositoryRoot, "");
  const apiMockingValue =
    process.env.VITE_API_MOCKING ?? loadedEnv.VITE_API_MOCKING;
  const { apiMockingEnabled, apiUrl } = resolveDevelopmentApiSource({
    apiMockingValue,
    apiUrlValue: process.env.VITE_API_URL ?? loadedEnv.VITE_API_URL,
  });
  let mockWorkerOutputPath = "";

  return {
    define: {
      "import.meta.env.VITE_API_MOCKING": JSON.stringify(
        apiMockingEnabled ? "true" : "false",
      ),
    },
    envDir: repositoryRoot,
    plugins: [
      {
        name: "exclude-browser-api-mock-worker-from-production",
        apply: "build",
        configResolved(config) {
          mockWorkerOutputPath = path.resolve(
            config.root,
            config.build.outDir,
            "mockServiceWorker.js",
          );
        },
        async closeBundle() {
          if (mockWorkerOutputPath) {
            await rm(mockWorkerOutputPath, { force: true });
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
    server:
      apiUrl && !apiMockingEnabled
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
