import { Trans } from "@lingui/react/macro";
import { CheckCircle2, Landmark, UserPlus } from "lucide-react";
import type { ReactNode } from "react";

import {
  AppAlert,
  AppAlertDescription,
  AppAlertTitle,
} from "@/components/app/AppAlert";
import { AppBadge } from "@/components/app/AppBadge";
import { AppButton } from "@/components/app/AppButton";
import {
  AppCard,
  AppCardContent,
  AppCardFooter,
  AppCardHeader,
  AppCardTitle,
} from "@/components/app/AppCard";
import { AppText } from "@/components/app/AppText";
import type { PendingInvitation } from "@/domains/membership";

type InvitationResponseCardProps = Readonly<{
  accepted: boolean;
  invitation: PendingInvitation;
  isAccepting: boolean;
  onAccept: () => void;
  renderPolitiesLink: (
    label: ReactNode,
    variant?: "default" | "outline",
  ) => ReactNode;
}>;

export function InvitationResponseCard({
  accepted,
  invitation,
  isAccepting,
  onAccept,
  renderPolitiesLink,
}: InvitationResponseCardProps) {
  if (accepted) {
    return (
      <div aria-live="polite" className="space-y-4">
        <AppAlert>
          <CheckCircle2 aria-hidden="true" />
          <AppAlertTitle>
            <Trans>Invitation Accepted</Trans>
          </AppAlertTitle>
          <AppAlertDescription>
            <Trans>You joined {invitation.polityName} as a citizen.</Trans>
          </AppAlertDescription>
        </AppAlert>
        {renderPolitiesLink(<Trans>Return to Your Polities</Trans>)}
      </div>
    );
  }

  return (
    <AppCard>
      <AppCardHeader>
        <div className="mb-3 flex flex-wrap items-center gap-2">
          <AppBadge>
            <Trans>Invitation</Trans>
          </AppBadge>
          <AppBadge variant="outline">{invitation.receivedLabel}</AppBadge>
        </div>
        <AppCardTitle>
          <Trans>Join as a Citizen</Trans>
        </AppCardTitle>
      </AppCardHeader>
      <AppCardContent className="space-y-5">
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="flex min-w-0 gap-3 rounded-lg border p-4">
            <UserPlus
              aria-hidden="true"
              className="mt-0.5 size-5 shrink-0 text-muted-foreground"
            />
            <div className="min-w-0">
              <AppText variant="caption">
                <Trans>Invited By</Trans>
              </AppText>
              <AppText className="mt-1 break-words" variant="strong">
                {invitation.invitedBy}
              </AppText>
            </div>
          </div>
          <div className="flex min-w-0 gap-3 rounded-lg border p-4">
            <Landmark
              aria-hidden="true"
              className="mt-0.5 size-5 shrink-0 text-muted-foreground"
            />
            <div className="min-w-0">
              <AppText variant="caption">
                <Trans>Membership</Trans>
              </AppText>
              <AppText className="mt-1" variant="strong">
                <Trans>Citizen</Trans>
              </AppText>
            </div>
          </div>
        </div>

        <div className="rounded-lg bg-muted/50 p-4">
          <AppText variant="strong">
            <Trans>What Happens Next</Trans>
          </AppText>
          <AppText className="mt-1" variant="supporting">
            <Trans>
              Accepting adds this polity to your workspace. Its constitution
              determines which proposals, votes, and other government actions
              are available to you.
            </Trans>
          </AppText>
        </div>
      </AppCardContent>
      <AppCardFooter className="flex-wrap justify-end gap-2">
        {renderPolitiesLink(<Trans>Decide Later</Trans>, "outline")}
        <AppButton disabled={isAccepting} onClick={onAccept}>
          <Trans>Accept Invitation</Trans>
        </AppButton>
      </AppCardFooter>
    </AppCard>
  );
}
