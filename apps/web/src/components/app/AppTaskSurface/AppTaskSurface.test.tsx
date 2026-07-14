import { setupI18n } from "@lingui/core";
import { I18nProvider } from "@lingui/react";
import { act, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AppTaskSurface } from "@/components/app/AppTaskSurface";

const originalMatchMedia = window.matchMedia;
const i18n = setupI18n({ locale: "en", messages: { en: {} } });

function setDesktopViewport(matches: boolean) {
  Object.defineProperty(window, "matchMedia", {
    configurable: true,
    value: vi.fn().mockImplementation(() => ({
      addEventListener: vi.fn(),
      matches,
      media: "(min-width: 640px)",
      removeEventListener: vi.fn(),
    })),
    writable: true,
  });
}

function renderTaskSurface() {
  return render(
    <I18nProvider i18n={i18n}>
      <AppTaskSurface
        describedBy="task-description"
        labelledBy="task-title"
        onDismiss={vi.fn()}
        open
      >
        <h2 id="task-title">Review invitation</h2>
        <p id="task-description">Invitation details</p>
      </AppTaskSurface>
    </I18nProvider>,
  );
}

afterEach(() => {
  vi.restoreAllMocks();
  Object.defineProperty(window, "matchMedia", {
    configurable: true,
    value: originalMatchMedia,
    writable: true,
  });
});

describe("AppTaskSurface", () => {
  it("opens after its closed state has been committed", () => {
    const animationFrames: FrameRequestCallback[] = [];
    setDesktopViewport(false);
    vi.spyOn(window, "requestAnimationFrame").mockImplementation((callback) => {
      animationFrames.push(callback);
      return animationFrames.length;
    });
    vi.spyOn(window, "cancelAnimationFrame").mockImplementation(() => {});

    renderTaskSurface();

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();

    act(() => animationFrames[0]?.(0));

    expect(screen.getByRole("dialog")).toHaveAttribute(
      "data-slot",
      "drawer-popup",
    );
  });

  it("uses a centered dialog at the desktop breakpoint", async () => {
    setDesktopViewport(true);

    renderTaskSurface();

    expect(await screen.findByRole("dialog")).toHaveAttribute(
      "data-slot",
      "dialog-content",
    );
  });

  it("uses a bottom drawer below the desktop breakpoint", async () => {
    setDesktopViewport(false);

    renderTaskSurface();

    expect(await screen.findByRole("dialog")).toHaveAttribute(
      "data-slot",
      "drawer-popup",
    );
  });
});
