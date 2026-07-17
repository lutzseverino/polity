import { Trans } from "@lingui/react/macro";
import { CheckCircle2 } from "lucide-react";
import type { ReactNode } from "react";

import {
  AppAlert,
  AppAlertDescription,
  AppAlertTitle,
} from "@/components/app/AppAlert";
import { AppButton } from "@/components/app/AppButton";
import { AppText } from "@/components/app/AppText";
import {
  type MembershipInvitation,
  MembershipInvitationDetails,
} from "@/domains/membership";
import { useAcceptMembershipInvitation } from "@/features/accept-membership-invitation/api/accept-membership-invitation-mutation";

export const acceptMembershipInvitationDescriptionId =
  "accept-membership-invitation-description";
export const acceptMembershipInvitationTitleId =
  "accept-membership-invitation-title";

type AcceptMembershipInvitationWorkflowProps = Readonly<{
  headingLevel: "h1" | "h2";
  invitation: MembershipInvitation;
  locale: string;
  onDismiss: () => void;
  renderPolitiesLink: (label: ReactNode) => ReactNode;
  showDismissAfterAccept?: boolean;
}>;

export function AcceptMembershipInvitationWorkflow({
  headingLevel,
  invitation,
  locale,
  onDismiss,
  renderPolitiesLink,
  showDismissAfterAccept = false,
}: AcceptMembershipInvitationWorkflowProps) {
  const acceptMembershipInvitation = useAcceptMembershipInvitation({ locale });
  const accepted = acceptMembershipInvitation.isSuccess;

  return (
    <div className="flex min-h-0 flex-col">
      <header className="shrink-0 px-5 pt-5 pr-14 pb-4 sm:px-6 sm:pt-6 sm:pr-16">
        <AppText variant="eyebrow">
          {accepted ? (
            <Trans>Invitation accepted</Trans>
          ) : (
            <Trans>Invitation to join</Trans>
          )}
        </AppText>
        <AppText
          as={headingLevel}
          className="mt-2"
          id={acceptMembershipInvitationTitleId}
          variant="contentTitle"
        >
          {accepted ? (
            <Trans>You joined {invitation.polityName}</Trans>
          ) : (
            <Trans>Join {invitation.polityName}?</Trans>
          )}
        </AppText>
        <AppText
          className="mt-2 max-w-prose"
          id={acceptMembershipInvitationDescriptionId}
          variant="supporting"
        >
          {accepted ? (
            <Trans>Your membership is now active.</Trans>
          ) : (
            <Trans>
              {invitation.invitedByName} invited you to join this polity.
            </Trans>
          )}
        </AppText>
      </header>

      <div className="min-h-0 overflow-y-auto px-5 pb-5 sm:px-6 sm:pb-6">
        {accepted ? (
          <AppAlert aria-live="polite">
            <CheckCircle2 aria-hidden="true" />
            <AppAlertTitle>
              <Trans>You’re now a member</Trans>
            </AppAlertTitle>
            <AppAlertDescription>
              <Trans>
                You can now participate according to the polity’s constitution.
              </Trans>
            </AppAlertDescription>
          </AppAlert>
        ) : (
          <MembershipInvitationDetails invitation={invitation} />
        )}
      </div>

      <footer className="flex shrink-0 flex-col-reverse gap-2 border-t bg-muted/50 px-5 pt-4 pb-[max(1rem,env(safe-area-inset-bottom))] sm:flex-row sm:justify-end sm:px-6 sm:pb-4">
        {accepted ? (
          <>
            {showDismissAfterAccept ? (
              <AppButton
                className="min-h-11 w-full sm:min-h-9 sm:w-auto"
                onClick={onDismiss}
                size="lg"
                variant="outline"
              >
                <Trans>Done</Trans>
              </AppButton>
            ) : null}
            {renderPolitiesLink(<Trans>View My Polities</Trans>)}
          </>
        ) : (
          <>
            <AppButton
              className="min-h-11 w-full sm:min-h-9 sm:w-auto"
              onClick={onDismiss}
              size="lg"
              variant="outline"
            >
              <Trans>Not Now</Trans>
            </AppButton>
            <AppButton
              className="min-h-11 w-full sm:min-h-9 sm:w-auto"
              disabled={acceptMembershipInvitation.isPending}
              onClick={() =>
                acceptMembershipInvitation.mutate({
                  invitationId: invitation.id,
                })
              }
              size="lg"
            >
              <Trans>Join polity</Trans>
            </AppButton>
          </>
        )}
      </footer>
    </div>
  );
}
