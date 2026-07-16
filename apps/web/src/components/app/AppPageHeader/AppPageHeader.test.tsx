import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { AppPageHeader } from "@/components/app/AppPageHeader";

describe("AppPageHeader", () => {
  it("exposes a semantic page heading to the shell visibility system", () => {
    render(<AppPageHeader title="Polities" />);

    const heading = screen.getByRole("heading", {
      level: 1,
      name: "Polities",
    });
    const header = heading.closest("header");

    expect(header).toHaveAttribute("data-slot", "page-header");
    expect(header?.querySelectorAll("p")).toHaveLength(0);
  });
});
