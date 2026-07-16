import { setupI18n } from "@lingui/core";
import { I18nProvider } from "@lingui/react";
import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import {
  AppDialog,
  AppDialogClose,
  AppDialogContent,
  AppDialogDescription,
  AppDialogTitle,
  AppDialogTrigger,
} from "@/components/app/AppDialog";

const originalMatchMedia = window.matchMedia;
const originalInnerWidth = window.innerWidth;
const i18n = setupI18n({ locale: "en", messages: { en: {} } });

function setMobileViewport(matches: boolean) {
  Object.defineProperty(window, "innerWidth", {
    configurable: true,
    value: matches ? 500 : 1024,
    writable: true,
  });
  Object.defineProperty(window, "matchMedia", {
    configurable: true,
    value: vi.fn().mockImplementation(() => ({
      addEventListener: vi.fn(),
      matches,
      media: "(max-width: 767px)",
      removeEventListener: vi.fn(),
    })),
    writable: true,
  });
}

function createResponsiveViewport(initiallyMobile: boolean) {
  const listeners = new Set<() => void>();
  let matches = initiallyMobile;
  const mediaQueryList = {
    addEventListener: (_type: string, listener: () => void) => {
      listeners.add(listener);
    },
    get matches() {
      return matches;
    },
    media: "(max-width: 767px)",
    removeEventListener: (_type: string, listener: () => void) => {
      listeners.delete(listener);
    },
  } as unknown as MediaQueryList;

  const setMobile = (nextMatches: boolean) => {
    matches = nextMatches;
    Object.defineProperty(window, "innerWidth", {
      configurable: true,
      value: matches ? 500 : 1024,
      writable: true,
    });
    act(() =>
      listeners.forEach((listener) => {
        listener();
      }),
    );
  };

  Object.defineProperty(window, "matchMedia", {
    configurable: true,
    value: vi.fn(() => mediaQueryList),
    writable: true,
  });
  setMobile(initiallyMobile);

  return { setMobile };
}

function renderAppDialog() {
  return render(
    <AppDialog>
      <AppDialogTrigger>Review invitation</AppDialogTrigger>
      <AppDialogContent showCloseButton={false}>
        <AppDialogTitle>Join Sunday Supper Club?</AppDialogTitle>
        <AppDialogDescription>Invitation details</AppDialogDescription>
        <AppDialogClose>Close</AppDialogClose>
      </AppDialogContent>
    </AppDialog>,
  );
}

function renderAppDialogWithDefaultClose() {
  return render(
    <I18nProvider i18n={i18n}>
      <AppDialog>
        <AppDialogTrigger>Open settings</AppDialogTrigger>
        <AppDialogContent className="test-content">
          <AppDialogTitle>Settings</AppDialogTitle>
          <AppDialogDescription>Dialog settings</AppDialogDescription>
        </AppDialogContent>
      </AppDialog>
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
  Object.defineProperty(window, "innerWidth", {
    configurable: true,
    value: originalInnerWidth,
    writable: true,
  });
});

describe("AppDialog", () => {
  it("commits a closed mobile drawer before a controlled opening", () => {
    const animationFrames: FrameRequestCallback[] = [];
    setMobileViewport(true);
    vi.spyOn(window, "requestAnimationFrame").mockImplementation((callback) => {
      animationFrames.push(callback);
      return animationFrames.length;
    });
    vi.spyOn(window, "cancelAnimationFrame").mockImplementation(() => {});

    render(
      <AppDialog open>
        <AppDialogContent showCloseButton={false}>
          <AppDialogTitle>Settings</AppDialogTitle>
        </AppDialogContent>
      </AppDialog>,
    );

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();

    act(() => animationFrames.at(-1)?.(0));

    const drawer = screen.getByRole("dialog");
    expect(drawer).toHaveAttribute("data-slot", "drawer-popup");
    expect(drawer).toHaveAttribute("data-starting-style");
  });

  it("preserves dialog composition on desktop", async () => {
    const user = userEvent.setup();
    setMobileViewport(false);
    renderAppDialog();

    await user.click(screen.getByRole("button", { name: "Review invitation" }));

    expect(screen.getByRole("dialog")).toHaveAttribute(
      "data-slot",
      "dialog-content",
    );
    expect(
      screen.getByRole("heading", { name: "Join Sunday Supper Club?" }),
    ).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Close" }));

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("preserves the same composition in a draggable mobile drawer", async () => {
    const user = userEvent.setup();
    setMobileViewport(true);
    renderAppDialog();

    await user.click(screen.getByRole("button", { name: "Review invitation" }));

    expect(screen.getByRole("dialog")).toHaveAttribute(
      "data-slot",
      "drawer-popup",
    );
    expect(
      document.querySelector('[data-slot="drawer-swipe-handle"]'),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "Join Sunday Supper Club?" }),
    ).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Close" }));

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("preserves uncontrolled open state across responsive primitive changes", async () => {
    const user = userEvent.setup();
    const viewport = createResponsiveViewport(false);
    renderAppDialog();

    await user.click(screen.getByRole("button", { name: "Review invitation" }));

    expect(screen.getByRole("dialog")).toHaveAttribute(
      "data-slot",
      "dialog-content",
    );

    viewport.setMobile(true);

    expect(await screen.findByRole("dialog")).toHaveAttribute(
      "data-slot",
      "drawer-popup",
    );

    viewport.setMobile(false);

    expect(screen.getByRole("dialog")).toHaveAttribute(
      "data-slot",
      "dialog-content",
    );
  });

  it.each([
    ["desktop", false],
    ["mobile", true],
  ] as const)("provides the app close affordance on %s while forwarding content props", async (_viewport, isMobile) => {
    const user = userEvent.setup();
    setMobileViewport(isMobile);
    renderAppDialogWithDefaultClose();

    await user.click(screen.getByRole("button", { name: "Open settings" }));

    expect(screen.getByRole("dialog")).toHaveClass("test-content");

    await user.click(screen.getByRole("button", { name: "Close" }));

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });
});
