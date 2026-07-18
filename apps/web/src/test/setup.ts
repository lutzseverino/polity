import "@testing-library/jest-dom/vitest";

import { cleanup } from "@testing-library/react";
import { afterAll, afterEach, beforeAll, vi } from "vitest";

import {
  apiMockServer,
  resetDefaultApiMockHandlers,
} from "@/test/mocks/server";

Object.defineProperty(window, "scrollTo", {
  value: vi.fn(),
  writable: true,
});

Object.defineProperty(window, "matchMedia", {
  value: vi.fn().mockImplementation((query: string) => ({
    addEventListener: vi.fn(),
    addListener: vi.fn(),
    dispatchEvent: vi.fn(),
    matches: query.includes("min-width"),
    media: query,
    onchange: null,
    removeEventListener: vi.fn(),
    removeListener: vi.fn(),
  })),
  writable: true,
});

beforeAll(() => {
  apiMockServer.listen({ onUnhandledRequest: "error" });
});

afterEach(() => {
  cleanup();
  resetDefaultApiMockHandlers();
});

afterAll(() => {
  apiMockServer.close();
});
