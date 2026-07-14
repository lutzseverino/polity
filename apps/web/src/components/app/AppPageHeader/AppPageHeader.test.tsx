import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { AppPageHeader } from "@/components/app/AppPageHeader";

describe("AppPageHeader", () => {
  it("keeps a compactly hidden title available to assistive technology", () => {
    render(<AppPageHeader compactVisibility="hidden" title="Polities" />);

    const heading = screen.getByRole("heading", {
      level: 1,
      name: "Polities",
    });
    const header = heading.closest("header");

    expect(header).toHaveClass("sr-only", "md:not-sr-only");
    expect(header?.querySelectorAll("p")).toHaveLength(0);
  });
});
