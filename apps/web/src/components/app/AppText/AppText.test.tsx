import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { AppText } from "@/components/app/AppText";

describe("AppText", () => {
  it("keeps semantic markup independent from the visual variant", () => {
    render(
      <AppText as="h1" variant="pageTitle">
        Polities
      </AppText>,
    );

    expect(
      screen.getByRole("heading", { level: 1, name: "Polities" }),
    ).toBeInTheDocument();
  });

  it("preserves layout classes supplied by its owner", () => {
    render(
      <AppText className="mt-2 max-w-xl" variant="supporting">
        Supporting copy
      </AppText>,
    );

    expect(screen.getByText("Supporting copy")).toHaveClass(
      "mt-2",
      "max-w-xl",
      "text-sm",
      "text-muted-foreground",
    );
  });
});
