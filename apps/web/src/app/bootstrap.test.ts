import { describe, expect, it, vi } from "vitest";

import { isBrowserApiMockingEnabled, startApplication } from "@/app/bootstrap";

describe("application bootstrap", () => {
  it("enables browser API mocking only for the explicit development option", () => {
    expect(
      isBrowserApiMockingEnabled({ development: true, value: "true" }),
    ).toBe(true);
    expect(
      isBrowserApiMockingEnabled({ development: true, value: "false" }),
    ).toBe(false);
    expect(isBrowserApiMockingEnabled({ development: true })).toBe(false);
    expect(
      isBrowserApiMockingEnabled({ development: false, value: "true" }),
    ).toBe(false);
  });

  it("waits for development setup before rendering", async () => {
    const order: string[] = [];
    let finishSetup: (() => void) | undefined;
    const setup = vi.fn(
      () =>
        new Promise<void>((resolve) => {
          finishSetup = () => {
            order.push("setup");
            resolve();
          };
        }),
    );
    const render = vi.fn(() => order.push("render"));

    const started = startApplication({ beforeRender: setup, render });

    expect(setup).toHaveBeenCalledOnce();
    expect(render).not.toHaveBeenCalled();

    finishSetup?.();
    await started;

    expect(order).toEqual(["setup", "render"]);
  });
});
