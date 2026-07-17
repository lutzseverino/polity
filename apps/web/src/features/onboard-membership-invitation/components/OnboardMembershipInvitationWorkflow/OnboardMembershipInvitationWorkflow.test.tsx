import { setupI18n } from "@lingui/core";
import { I18nProvider } from "@lingui/react";
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { MembershipInvitationCompletion } from "@/domains/membership";
import { shouldPollMembershipInvitationCompletion } from "@/features/onboard-membership-invitation/api/membership-invitation-completion";
import { MembershipInvitationOnboardingContent } from "@/features/onboard-membership-invitation/components/OnboardMembershipInvitationWorkflow/OnboardMembershipInvitationWorkflow";

const i18n = setupI18n({ locale: "en", messages: { en: {} } });
const invitation = {
  expiresAt: "2026-07-20T10:00:00Z",
  invitedEmail: "friend@example.com",
  polityId: "polity-1",
  polityName: "Friend Republic",
};

function completion(
  status: MembershipInvitationCompletion["status"],
): MembershipInvitationCompletion {
  return {
    attemptCount: 1,
    createdAt: "2026-07-18T10:00:00Z",
    status,
    updatedAt: "2026-07-18T10:01:00Z",
  };
}

function renderContent(
  state?: MembershipInvitationCompletion,
  onRequestCompletion = vi.fn(),
) {
  render(
    <I18nProvider i18n={i18n}>
      <MembershipInvitationOnboardingContent
        completion={state}
        invitation={invitation}
        isPending={false}
        onRequestCompletion={onRequestCompletion}
        renderSignInLink={(label) => <a href="/sign-in">{label}</a>}
      />
    </I18nProvider>,
  );
  return onRequestCompletion;
}

describe("membership invitation onboarding", () => {
  it("starts passwordless identity setup without presenting credential fields", () => {
    const request = renderContent();

    fireEvent.click(screen.getByRole("button", { name: "Set up identity" }));

    expect(request).toHaveBeenCalledOnce();
    expect(screen.queryByLabelText(/password/i)).not.toBeInTheDocument();
  });

  it("stops at identity completion and hands off to sign-in", () => {
    renderContent(completion("completed"));

    expect(screen.getByText("Your identity is ready")).toBeInTheDocument();
    expect(
      screen.getByText(/does not accept polity membership/i),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Sign in to review invitation" }),
    ).toHaveAttribute("href", "/sign-in");
  });

  it("shows Cardo's terminal failure and permits an explicit retry", () => {
    const request = vi.fn();
    renderContent(
      { ...completion("failed"), lastError: "credential_action_expired" },
      request,
    );

    expect(screen.getByText("credential_action_expired")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Try again" }));
    expect(request).toHaveBeenCalledOnce();
  });

  it("polls only Cardo's non-terminal completion states", () => {
    expect(shouldPollMembershipInvitationCompletion("requested")).toBe(true);
    expect(shouldPollMembershipInvitationCompletion("awaiting_identity")).toBe(
      true,
    );
    expect(shouldPollMembershipInvitationCompletion("completed")).toBe(false);
    expect(shouldPollMembershipInvitationCompletion("failed")).toBe(false);
  });
});
