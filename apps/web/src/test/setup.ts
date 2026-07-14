import "@testing-library/jest-dom/vitest";

import { cleanup } from "@testing-library/react";
import { afterAll, afterEach, beforeAll, vi } from "vitest";

import { apiMockServer } from "@/test/mocks/server";

Object.defineProperty(window, "scrollTo", {
  value: vi.fn(),
  writable: true,
});

beforeAll(() => {
  apiMockServer.listen({ onUnhandledRequest: "error" });
});

afterEach(() => {
  cleanup();
  apiMockServer.resetHandlers();
});

afterAll(() => {
  apiMockServer.close();
});
