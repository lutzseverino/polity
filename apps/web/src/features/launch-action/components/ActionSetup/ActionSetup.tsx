import { Trans, useLingui } from "@lingui/react/macro";
import { ShieldCheck } from "lucide-react";

import { AppBackLink } from "@/components/app/AppBackLink";
import { AppBadge } from "@/components/app/AppBadge";
import {
  AppCard,
  AppCardContent,
  AppCardDescription,
  AppCardHeader,
  AppCardTitle,
} from "@/components/app/AppCard";
import { AppPageHeader } from "@/components/app/AppPageHeader";
import { AppText } from "@/components/app/AppText";
import { findActionDefinition } from "@/features/launch-action/lib/action-definitions";
import type { PolityOption } from "@/features/launch-action/lib/launch-action";

type ActionSetupProps = Readonly<{
  actionId?: string;
  polities: readonly PolityOption[];
  polityId?: string;
}>;

export function ActionSetup({
  actionId,
  polities,
  polityId,
}: ActionSetupProps) {
  const { i18n, t } = useLingui();
  const action = findActionDefinition(actionId ?? null);
  const polity = polities.find((option) => option.id === polityId);

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <AppBackLink to="/home">
        <Trans>Back to Home</Trans>
      </AppBackLink>
      <AppPageHeader
        description={
          action ? (
            i18n._(action.description)
          ) : (
            <Trans>
              Return to the launcher and choose a valid action to continue.
            </Trans>
          )
        }
        eyebrow={<Trans>Official Action</Trans>}
        title={action ? i18n._(action.label) : <Trans>Choose an Action</Trans>}
      />

      <AppCard>
        <AppCardHeader>
          <div className="mb-2 flex flex-wrap gap-2">
            <AppBadge>{polity?.name ?? t`No polity selected`}</AppBadge>
            <AppBadge variant="outline">
              <Trans>Draft</Trans>
            </AppBadge>
          </div>
          <AppCardTitle>
            <Trans>Review Before You Submit</Trans>
          </AppCardTitle>
          <AppCardDescription>
            <Trans>
              This handoff reserves space for the structured form required by
              the selected action.
            </Trans>
          </AppCardDescription>
        </AppCardHeader>
        <AppCardContent className="flex gap-3 rounded-lg bg-muted/60 p-4">
          <ShieldCheck aria-hidden="true" className="mt-0.5 size-5 shrink-0" />
          <AppText variant="supporting">
            <Trans>
              Your words will never become a binding government action
              automatically. You will review the action type, polity, formal
              content, eligibility, and consequences first.
            </Trans>
          </AppText>
        </AppCardContent>
      </AppCard>
    </div>
  );
}
