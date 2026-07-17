import { setupI18n } from "@lingui/core";
import { I18nProvider } from "@lingui/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";

import { AcceptMembershipInvitationWorkflow } from "@/features/accept-membership-invitation/components/AcceptMembershipInvitationWorkflow/AcceptMembershipInvitationWorkflow";
import { apiMockServer } from "@/test/mocks/server";

const i18n = setupI18n({ locale: "en", messages: { en: {} } });

describe("accept membership invitation mutation", () => {
  it("shows a retryable error when Polity rejects acceptance", async () => {
    apiMockServer.use(
      http.post("/api/v1/invitations/:invitationId/accept", () =>
        HttpResponse.json(
          { code: "invitation_not_ready", message: "Invitation is not ready." },
          { status: 409 },
        ),
      ),
    );
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    const user = userEvent.setup();
    render(
      <I18nProvider i18n={i18n}>
        <QueryClientProvider client={queryClient}>
          <AcceptMembershipInvitationWorkflow
            headingLevel="h1"
            invitation={{
              id: "invitation-1",
              invitedAtLabel: "Jul 17, 2026",
              invitedByName: "Mira Chen",
              polityName: "Garden Cooperative",
            }}
            locale="en"
            onDismiss={() => undefined}
            renderPolitiesLink={(label) => <a href="/polities">{label}</a>}
          />
        </QueryClientProvider>
      </I18nProvider>,
    );

    await user.click(screen.getByRole("button", { name: "Join polity" }));

    expect(
      (await screen.findByText("Couldn’t join polity")).closest(
        '[role="alert"]',
      ),
    ).toBeInTheDocument();
    expect(screen.queryByText("You’re now a member")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Join polity" })).toBeEnabled();
  });
});
