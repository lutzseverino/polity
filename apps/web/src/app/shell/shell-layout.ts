import { useSyncExternalStore } from "react";

export type ShellLayout = "compact" | "expanded" | "medium";

const mediumShellMediaQuery = "(min-width: 48rem)";
const expandedShellMediaQuery = "(min-width: 80rem)";

export const shellContentInsetClassName: Readonly<Record<ShellLayout, string>> =
  {
    compact: "",
    expanded: "ml-60",
    medium: "ml-20",
  };

function readShellLayout(): ShellLayout {
  if (
    typeof window === "undefined" ||
    typeof window.matchMedia !== "function"
  ) {
    return "expanded";
  }

  if (window.matchMedia(expandedShellMediaQuery).matches) {
    return "expanded";
  }

  if (window.matchMedia(mediumShellMediaQuery).matches) {
    return "medium";
  }

  return "compact";
}

function subscribeToShellLayout(onChange: () => void) {
  if (
    typeof window === "undefined" ||
    typeof window.matchMedia !== "function"
  ) {
    return () => undefined;
  }

  const mediaQueries = [
    window.matchMedia(mediumShellMediaQuery),
    window.matchMedia(expandedShellMediaQuery),
  ];

  for (const mediaQuery of mediaQueries) {
    mediaQuery.addEventListener("change", onChange);
  }

  return () => {
    for (const mediaQuery of mediaQueries) {
      mediaQuery.removeEventListener("change", onChange);
    }
  };
}

export function useShellLayout() {
  return useSyncExternalStore(
    subscribeToShellLayout,
    readShellLayout,
    readShellLayout,
  );
}
