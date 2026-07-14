import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { AppSeparator } from "@/components/app/AppSeparator";

describe("AppSeparator", () => {
  it("offers an explicit gradient treatment", () => {
    render(<AppSeparator variant="gradient" />);

    expect(screen.getByRole("separator")).toHaveClass(
      "data-horizontal:bg-linear-to-r",
      "data-vertical:bg-linear-to-b",
    );
  });

  it("uses the primitive's solid treatment by default", () => {
    render(<AppSeparator />);

    expect(screen.getByRole("separator")).not.toHaveClass(
      "data-horizontal:bg-linear-to-r",
    );
  });
});
