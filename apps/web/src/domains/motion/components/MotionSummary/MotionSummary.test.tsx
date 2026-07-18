import { setupI18n } from "@lingui/core";
import { I18nProvider } from "@lingui/react";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { MotionSummary } from "@/domains/motion/components/MotionSummary/MotionSummary";

const i18n = setupI18n({ locale: "en", messages: { en: {} } });

describe("MotionSummary", () => {
  it("renders a supplied motion and preserves the consumer action slot", () => {
    render(
      <I18nProvider i18n={i18n}>
        <MotionSummary
          action={<span>Open motion</span>}
          motion={{
            actionAvailability: { available: true },
            actionKind: "vote",
            body: "Approve the shared meal budget.",
            category: "Resolution",
            closesAtLabel: "Tomorrow",
            id: "shared-meal",
            introducedBy: "Sam Ortega",
            participation: {
              cast: 3,
              eligible: 8,
              quorumMet: false,
              quorumRequired: 5,
            },
            procedure: {
              electorate: "Members",
              name: "Simple majority",
              notice: "Three days",
              threshold: "More yes than no",
            },
            status: "voting",
            title: "Shared meal budget",
          }}
        />
      </I18nProvider>,
    );

    expect(screen.getByText("Shared meal budget")).toBeInTheDocument();
    expect(screen.getByText("Open motion")).toBeInTheDocument();
    expect(screen.getByText("Voting open")).toBeInTheDocument();
  });
});
