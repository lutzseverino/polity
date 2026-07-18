import { describe, expect, it, vi } from "vitest";

import { materializeBrowserSessionScenarioCsrf } from "@/mocks/scenarios/session";
import { handleUnhandledBrowserRequest } from "@/mocks/unhandled-request";
import { setTestCookie } from "@/test/cookies";

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

  it("materializes the readable CSRF cookie that service-worker responses cannot set", () => {
    setTestCookie("cardo.csrf=; Max-Age=0; Path=/");

    materializeBrowserSessionScenarioCsrf();

    expect(document.cookie).toContain("cardo.csrf=mock-csrf-token");
  });
});
