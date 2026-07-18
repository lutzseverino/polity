import { Trans, useLingui } from "@lingui/react/macro";
import { Check } from "lucide-react";
import { useState } from "react";

import {
  AppAlert,
  AppAlertDescription,
  AppAlertTitle,
} from "@/components/app/AppAlert";
import { AppButton } from "@/components/app/AppButton";
import {
  AppCard,
  AppCardContent,
  AppCardDescription,
  AppCardHeader,
  AppCardTitle,
} from "@/components/app/AppCard";
import type { Motion } from "@/domains/motion";
import { useRespondOfficeElectionCandidacy } from "@/features/respond-office-election-candidacy/api/respond-office-election-candidacy-mutation";
import type { OfficeElectionCandidacyResponse } from "@/features/respond-office-election-candidacy/api/respond-office-election-candidacy-request";

type RespondOfficeElectionCandidacyWorkflowProps = Readonly<{
  motion: Motion;
  polityId: string;
}>;

export function RespondOfficeElectionCandidacyWorkflow({
  motion,
  polityId,
}: RespondOfficeElectionCandidacyWorkflowProps) {
  const [response, setResponse] =
    useState<OfficeElectionCandidacyResponse | null>(null);
  const { i18n } = useLingui();
  const respondOfficeElectionCandidacy = useRespondOfficeElectionCandidacy(
    i18n.locale,
  );

  function respond(nextResponse: OfficeElectionCandidacyResponse) {
    respondOfficeElectionCandidacy.mutate(
      { motionId: motion.id, polityId, response: nextResponse },
      {
        onSuccess: ({ response: recordedResponse }) =>
          setResponse(recordedResponse),
      },
    );
  }

  return (
    <AppCard>
      <AppCardHeader>
        <AppCardTitle>
          <Trans>Respond to your nomination</Trans>
        </AppCardTitle>
        <AppCardDescription>
          <Trans>
            Only accepted candidates can appear on ballots or be elected.
          </Trans>
        </AppCardDescription>
      </AppCardHeader>
      <AppCardContent className="space-y-4">
        <div className="flex flex-col gap-2 sm:flex-row">
          <AppButton
            disabled={respondOfficeElectionCandidacy.isPending}
            onClick={() => respond("accepted")}
          >
            <Trans>Accept nomination</Trans>
          </AppButton>
          <AppButton
            disabled={respondOfficeElectionCandidacy.isPending}
            onClick={() => respond("declined")}
            variant="outline"
          >
            <Trans>Decline</Trans>
          </AppButton>
        </div>
        {response ? (
          <AppAlert>
            <Check aria-hidden="true" />
            <AppAlertTitle>
              <Trans>Response recorded</Trans>
            </AppAlertTitle>
            <AppAlertDescription>
              <Trans>
                You {response} the nomination. You can update your response
                until {motion.closesAtLabel.toLowerCase()}.
              </Trans>
            </AppAlertDescription>
          </AppAlert>
        ) : null}
      </AppCardContent>
    </AppCard>
  );
}
