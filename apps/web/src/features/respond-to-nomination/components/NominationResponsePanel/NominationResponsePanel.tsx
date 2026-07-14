import { Trans } from "@lingui/react/macro";
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
import { useRespondToNomination } from "@/features/respond-to-nomination/api/respond-to-nomination-mutation";
import type { NominationResponse } from "@/features/respond-to-nomination/api/respond-to-nomination-request";

type NominationResponsePanelProps = Readonly<{
  motion: Motion;
  polityId: string;
}>;

export function NominationResponsePanel({
  motion,
  polityId,
}: NominationResponsePanelProps) {
  const [response, setResponse] = useState<NominationResponse | null>(null);
  const respondToNomination = useRespondToNomination();

  function respond(nextResponse: NominationResponse) {
    respondToNomination.mutate(
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
            disabled={respondToNomination.isPending}
            onClick={() => respond("accepted")}
          >
            <Trans>Accept nomination</Trans>
          </AppButton>
          <AppButton
            disabled={respondToNomination.isPending}
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
