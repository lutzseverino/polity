import { setupI18n } from "@lingui/core";
import { I18nProvider } from "@lingui/react";
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { MembershipInvitationCompletion } from "@/domains/membership";
import { shouldPollMembershipInvitationCompletion } from "@/features/onboard-membership-invitation/api/membership-invitation-completion";
import { MembershipInvitationOnboardingContent } from "@/features/onboard-membership-invitation/components/OnboardMembershipInvitationWorkflow/OnboardMembershipInvitationWorkflow";

const i18n = setupI18n({ locale: "en", messages: { en: {} } });
const invitation = {
  expiresAtLabel: "July 20, 2026",
  invitedEmail: "friend@example.com",
  polityId: "polity-1",
  polityName: "Friend Republic",
};

function completion(
  status: MembershipInvitationCompletion["status"],
): MembershipInvitationCompletion {
  return { status };
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
  it("starts passwordless signup without presenting credential fields", () => {
    const request = renderContent();

    fireEvent.click(screen.getByRole("button", { name: "Sign up" }));

    expect(request).toHaveBeenCalledOnce();
    expect(screen.queryByLabelText(/password/i)).not.toBeInTheDocument();
  });

  it("stops at signup completion and hands off to login", () => {
    renderContent(completion("completed"));

    expect(screen.getByText("You’re signed up")).toBeInTheDocument();
    expect(
      screen.getByText(/won’t join until you accept it/i),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Log in to review invitation" }),
    ).toHaveAttribute("href", "/sign-in");
  });

  it("presents pending setup without implementation language", () => {
    renderContent(completion("awaiting_identity"));

    expect(screen.getByText("Check your email")).toBeInTheDocument();
    expect(screen.getByText(/finish signing up/i)).toBeInTheDocument();
    expect(screen.queryByText(/cardo|keycloak/i)).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sign up" })).toBeDisabled();
  });

  it("shows a product-owned terminal failure and permits an explicit retry", () => {
    const request = vi.fn();
    renderContent(completion("failed"), request);

    expect(screen.getByText(/something went wrong/i)).toBeInTheDocument();
    expect(
      screen.queryByText("credential_action_expired"),
    ).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Try again" }));
    expect(request).toHaveBeenCalledOnce();
  });

  it("shows a product-owned request error and retry affordance", () => {
    const request = vi.fn();
    render(
      <I18nProvider i18n={i18n}>
        <MembershipInvitationOnboardingContent
          error={new Error("upstream_internal_failure")}
          invitation={invitation}
          isPending={false}
          onRequestCompletion={request}
          renderSignInLink={(label) => <a href="/sign-in">{label}</a>}
        />
      </I18nProvider>,
    );

    expect(
      screen.queryByText("upstream_internal_failure"),
    ).not.toBeInTheDocument();
    expect(screen.getByText(/check your connection/i)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Try again" }));
    expect(request).toHaveBeenCalledOnce();
  });

  it("polls only non-terminal completion states", () => {
    expect(shouldPollMembershipInvitationCompletion("requested")).toBe(true);
    expect(shouldPollMembershipInvitationCompletion("awaiting_identity")).toBe(
      true,
    );
    expect(shouldPollMembershipInvitationCompletion("completed")).toBe(false);
    expect(shouldPollMembershipInvitationCompletion("failed")).toBe(false);
  });
});
