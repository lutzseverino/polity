import { Trans } from "@lingui/react/macro";
import { CalendarClock, Info, UserPlus } from "lucide-react";

import {
  AppAlert,
  AppAlertDescription,
  AppAlertTitle,
} from "@/components/app/AppAlert";
import { AppText } from "@/components/app/AppText";
import type { MembershipInvitation } from "@/domains/membership/lib/membership";

type MembershipInvitationDetailsProps = Readonly<{
  invitation: MembershipInvitation;
}>;

export function MembershipInvitationDetails({
  invitation,
}: MembershipInvitationDetailsProps) {
  return (
    <div className="space-y-5">
      <dl className="grid gap-3 sm:grid-cols-2">
        <div className="flex min-w-0 gap-3 rounded-lg border p-4">
          <UserPlus
            aria-hidden="true"
            className="mt-0.5 size-5 shrink-0 text-muted-foreground"
          />
          <div className="min-w-0">
            <AppText as="dt" variant="caption">
              <Trans>Invited by</Trans>
            </AppText>
            <AppText as="dd" className="mt-1 break-words" variant="strong">
              {invitation.invitedByName}
            </AppText>
          </div>
        </div>
        <div className="flex min-w-0 gap-3 rounded-lg border p-4">
          <CalendarClock
            aria-hidden="true"
            className="mt-0.5 size-5 shrink-0 text-muted-foreground"
          />
          <div className="min-w-0">
            <AppText as="dt" variant="caption">
              <Trans>Received</Trans>
            </AppText>
            <AppText as="dd" className="mt-1" variant="strong">
              {invitation.invitedAtLabel}
            </AppText>
          </div>
        </div>
      </dl>

      <AppAlert>
        <Info aria-hidden="true" />
        <AppAlertTitle>
          <Trans>What joining changes</Trans>
        </AppAlertTitle>
        <AppAlertDescription>
          <Trans>
            This polity will be added to your workspace. Its constitution
            determines which proposals, votes, and other government actions are
            available to you.
          </Trans>
        </AppAlertDescription>
      </AppAlert>
    </div>
  );
}
