import { describe, expect, it } from "vitest";

import {
  type ShellLayout,
  shouldHideShellPageHeader,
} from "@/app/shell/shell-layout";
import type { ShellRouteLevel } from "@/app/shell/shell-route-context";

const layouts: readonly ShellLayout[] = ["compact", "medium", "expanded"];
const levels: readonly ShellRouteLevel[] = [
  "root",
  "workspace",
  "detail",
  "task",
];

describe("shell page-header visibility", () => {
  it.each(
    layouts.flatMap((layout) =>
      levels.map((level) => ({
        hidden: layout === "compact" && level === "root",
        layout,
        level,
      })),
    ),
  )("resolves $layout/$level visibility", ({ hidden, layout, level }) => {
    expect(shouldHideShellPageHeader(layout, level)).toBe(hidden);
  });
});
