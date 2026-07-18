import { describe, expect, it, vi } from "vitest";

import { handleUnhandledBrowserRequest } from "@/mocks/unhandled-request";

describe("browser API mock boundary", () => {
  it("fails unexpected same-origin API requests", () => {
    const print = { error: vi.fn() };

    handleUnhandledBrowserRequest(
      new Request(new URL("/api/v1/unhandled", window.location.href)),
      print,
    );

    expect(print.error).toHaveBeenCalledOnce();
  });

  it("allows non-API application resources through", () => {
    const print = { error: vi.fn() };

    handleUnhandledBrowserRequest(
      new Request(new URL("/src/main.tsx", window.location.href)),
      print,
    );

    expect(print.error).not.toHaveBeenCalled();
  });
});
