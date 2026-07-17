import { Trans } from "@lingui/react/macro";
import { AlertTriangle, CheckCircle2, MailCheck } from "lucide-react";
import type { ReactNode } from "react";
import {
  AppAlert,
  AppAlertDescription,
  AppAlertTitle,
} from "@/components/app/AppAlert";
import { AppButton } from "@/components/app/AppButton";
import { AppText } from "@/components/app/AppText";
import type {
  MembershipInvitationCompletion,
  MembershipInvitationTokenContext,
} from "@/domains/membership";
import { useMembershipInvitationCompletion } from "@/features/onboard-membership-invitation/api/membership-invitation-completion";

type OnboardingContentProps = Readonly<{
  completion?: MembershipInvitationCompletion;
  error?: Error | null;
  invitation: MembershipInvitationTokenContext;
  isPending: boolean;
  onRequestCompletion: () => void;
  renderSignInLink: (label: ReactNode) => ReactNode;
}>;

export function MembershipInvitationOnboardingContent({
  completion,
  error,
  invitation,
  isPending,
  onRequestCompletion,
  renderSignInLink,
}: OnboardingContentProps) {
  const failed = completion?.status === "failed";
  const completed = completion?.status === "completed";
  const inProgress =
    completion?.status === "requested" ||
    completion?.status === "awaiting_identity";

  return (
    <div className="flex flex-col">
      <header className="px-5 pt-5 pb-4 sm:px-6 sm:pt-6">
        <AppText variant="eyebrow">
          <Trans>Membership invitation</Trans>
        </AppText>
        <AppText as="h1" className="mt-2" variant="contentTitle">
          <Trans>Join {invitation.polityName}</Trans>
        </AppText>
        <AppText className="mt-2 max-w-prose" variant="supporting">
          <Trans>
            This invitation was sent to {invitation.invitedEmail}. Set up your
            identity, sign in, and then choose whether to accept membership.
          </Trans>
        </AppText>
      </header>

      <div className="space-y-4 px-5 pb-5 sm:px-6 sm:pb-6">
        {inProgress ? (
          <AppAlert aria-live="polite">
            <MailCheck aria-hidden="true" />
            <AppAlertTitle>
              <Trans>Check your email</Trans>
            </AppAlertTitle>
            <AppAlertDescription>
              <Trans>
                Cardo is preparing a secure Keycloak action. This page will
                update automatically.
              </Trans>
            </AppAlertDescription>
          </AppAlert>
        ) : null}

        {completed ? (
          <AppAlert aria-live="polite">
            <CheckCircle2 aria-hidden="true" />
            <AppAlertTitle>
              <Trans>Your identity is ready</Trans>
            </AppAlertTitle>
            <AppAlertDescription>
              <Trans>
                Sign in to review the pending invitation. Identity setup does
                not accept polity membership.
              </Trans>
            </AppAlertDescription>
          </AppAlert>
        ) : null}

        {failed ? (
          <AppAlert aria-live="assertive" variant="destructive">
            <AlertTriangle aria-hidden="true" />
            <AppAlertTitle>
              <Trans>Identity setup failed</Trans>
            </AppAlertTitle>
            <AppAlertDescription>
              {completion.lastError ?? (
                <Trans>Cardo could not complete identity setup.</Trans>
              )}
            </AppAlertDescription>
          </AppAlert>
        ) : null}

        {error ? (
          <AppAlert aria-live="assertive" variant="destructive">
            <AlertTriangle aria-hidden="true" />
            <AppAlertTitle>
              <Trans>Unable to continue</Trans>
            </AppAlertTitle>
            <AppAlertDescription>{error.message}</AppAlertDescription>
          </AppAlert>
        ) : null}

        <AppText variant="supporting">
          <Trans>Invitation expires {invitation.expiresAt}.</Trans>
        </AppText>
        {completion ? (
          <AppText variant="supporting">
            <Trans>Identity setup attempt {completion.attemptCount}.</Trans>
          </AppText>
        ) : null}
        {completion?.actionExpiresAt ? (
          <AppText variant="supporting">
            <Trans>
              The current identity action expires {completion.actionExpiresAt}.
            </Trans>
          </AppText>
        ) : null}
      </div>

      <footer className="flex justify-end border-t bg-muted/50 px-5 pt-4 pb-[max(1rem,env(safe-area-inset-bottom))] sm:px-6 sm:pb-4">
        {completed ? (
          renderSignInLink(<Trans>Sign in to review invitation</Trans>)
        ) : (
          <AppButton
            disabled={isPending || inProgress}
            onClick={onRequestCompletion}
            size="lg"
          >
            {failed ? <Trans>Try again</Trans> : <Trans>Set up identity</Trans>}
          </AppButton>
        )}
      </footer>
    </div>
  );
}

type OnboardMembershipInvitationWorkflowProps = Readonly<{
  invitation: MembershipInvitationTokenContext;
  renderSignInLink: (label: ReactNode) => ReactNode;
  token: string;
}>;

export function OnboardMembershipInvitationWorkflow({
  invitation,
  renderSignInLink,
  token,
}: OnboardMembershipInvitationWorkflowProps) {
  const completion = useMembershipInvitationCompletion(token);

  return (
    <MembershipInvitationOnboardingContent
      completion={completion.completion}
      error={completion.error}
      invitation={invitation}
      isPending={completion.isPending}
      onRequestCompletion={completion.request}
      renderSignInLink={renderSignInLink}
    />
  );
}
