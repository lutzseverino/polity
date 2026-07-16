import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { AppPageLayout } from "@/components/app/AppPageLayout";

describe("AppPageLayout", () => {
  it.each([
    ["wide", "max-w-none"],
    ["standard", "max-w-4xl"],
    ["focused", "max-w-3xl"],
    ["narrow", "max-w-2xl"],
  ] as const)("applies the %s page measure", (measure, measureClassName) => {
    render(
      <AppPageLayout className="test-layout" measure={measure}>
        Page content
      </AppPageLayout>,
    );

    const layout = screen.getByText("Page content");

    expect(layout).toHaveAttribute("data-measure", measure);
    expect(layout).toHaveAttribute("data-slot", "page-layout");
    expect(layout).toHaveClass(
      "flex",
      "w-full",
      "flex-col",
      "gap-6",
      measureClassName,
      "test-layout",
    );
    expect(layout).toHaveClass(measure === "wide" ? "max-w-none" : "mx-auto");
  });
});
