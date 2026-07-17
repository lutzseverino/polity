import { setupI18n } from "@lingui/core";
import { I18nProvider } from "@lingui/react";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { MembershipInvitationDetails } from "@/domains/membership/components/MembershipInvitationDetails/MembershipInvitationDetails";

const i18n = setupI18n({ locale: "en", messages: { en: {} } });

describe("MembershipInvitationDetails", () => {
  it("renders the supplied membership invitation as read-only details", () => {
    render(
      <I18nProvider i18n={i18n}>
        <MembershipInvitationDetails
          invitation={{
            id: "invitation-1",
            invitedAtLabel: "Yesterday",
            invitedByName: "Mira Chen",
            polityName: "Garden Cooperative",
          }}
        />
      </I18nProvider>,
    );

    expect(screen.getByText("Mira Chen")).toBeInTheDocument();
    expect(screen.getByText("Yesterday")).toBeInTheDocument();
    expect(screen.getByText("What joining changes")).toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });
});
