import { Trans } from "@lingui/react/macro";
import { CalendarClock, CheckCircle2, UserPlus } from "lucide-react";
import type { ReactNode } from "react";

import {
  AppAlert,
  AppAlertDescription,
  AppAlertTitle,
} from "@/components/app/AppAlert";
import { AppButton } from "@/components/app/AppButton";
import { AppText } from "@/components/app/AppText";
import { useInvitation } from "@/domains/membership";
import { useAcceptInvitation } from "@/features/accept-invitation/api/accept-invitation-mutation";

export const invitationResponseDescriptionId =
  "invitation-response-description";
export const invitationResponseTitleId = "invitation-response-title";

type InvitationResponseProps = Readonly<{
  descriptionId: string;
  headingLevel: "h1" | "h2";
  invitationId: string;
  locale: string;
  onDismiss: () => void;
  renderPolitiesLink: (label: ReactNode) => ReactNode;
  showDismissAfterAccept?: boolean;
  titleId: string;
}>;

export function InvitationResponse({
  descriptionId,
  headingLevel,
  invitationId,
  locale,
  onDismiss,
  renderPolitiesLink,
  showDismissAfterAccept = false,
  titleId,
}: InvitationResponseProps) {
  const { data: invitation } = useInvitation({ invitationId, locale });
  const acceptInvitation = useAcceptInvitation({ locale });
  const accepted = acceptInvitation.isSuccess;

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
          id={titleId}
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
          id={descriptionId}
          variant="supporting"
        >
          {accepted ? (
            <Trans>Your membership is now active.</Trans>
          ) : (
            <Trans>
              {invitation.invitedBy} invited you to join this polity.
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
          <div className="space-y-5">
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="flex min-w-0 gap-3 rounded-lg border p-4">
                <UserPlus
                  aria-hidden="true"
                  className="mt-0.5 size-5 shrink-0 text-muted-foreground"
                />
                <div className="min-w-0">
                  <AppText variant="caption">
                    <Trans>Invited by</Trans>
                  </AppText>
                  <AppText className="mt-1 break-words" variant="strong">
                    {invitation.invitedBy}
                  </AppText>
                </div>
              </div>
              <div className="flex min-w-0 gap-3 rounded-lg border p-4">
                <CalendarClock
                  aria-hidden="true"
                  className="mt-0.5 size-5 shrink-0 text-muted-foreground"
                />
                <div className="min-w-0">
                  <AppText variant="caption">
                    <Trans>Received</Trans>
                  </AppText>
                  <AppText className="mt-1" variant="strong">
                    {invitation.receivedLabel}
                  </AppText>
                </div>
              </div>
            </div>

            <div className="rounded-lg bg-muted/50 p-4">
              <AppText variant="strong">
                <Trans>What joining changes</Trans>
              </AppText>
              <AppText className="mt-1" variant="supporting">
                <Trans>
                  This polity will be added to your workspace. Its constitution
                  determines which proposals, votes, and other government
                  actions are available to you.
                </Trans>
              </AppText>
            </div>
          </div>
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
              disabled={acceptInvitation.isPending}
              onClick={() => acceptInvitation.mutate({ invitationId })}
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
