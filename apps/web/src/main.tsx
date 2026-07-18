import { StrictMode } from "react";
import { createRoot } from "react-dom/client";

import { App } from "@/app/App";
import { isBrowserApiMockingEnabled, startApplication } from "@/app/bootstrap";
import "@/index.css";

const root = document.getElementById("root");

if (!root) {
  throw new Error("Application root element not found.");
}

const beforeRender =
  import.meta.env.DEV &&
  isBrowserApiMockingEnabled({
    development: true,
    value: import.meta.env.VITE_API_MOCKING,
  })
    ? async () => {
        const { startBrowserApiMocking } = await import("@/mocks/browser");
        await startBrowserApiMocking();
      }
    : undefined;

void startApplication({
  beforeRender,
  render: () => {
    createRoot(root).render(
      <StrictMode>
        <App />
      </StrictMode>,
    );
  },
});
