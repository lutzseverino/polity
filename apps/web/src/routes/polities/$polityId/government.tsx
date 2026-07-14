import { Trans } from "@lingui/react/macro";
import { createFileRoute } from "@tanstack/react-router";

import { AppPlannedDestination } from "@/components/app/AppPlannedDestination";

export const Route = createFileRoute("/polities/$polityId/government")({
  component: GovernmentRoute,
});

function GovernmentRoute() {
  return (
    <AppPlannedDestination
      className="max-w-3xl"
      description={
        <Trans>
          Offices, people, constitutional rules, and government health will be
          developed after proceedings are validated.
        </Trans>
      }
      label={<Trans>Planned polity destination</Trans>}
      title={<Trans>Government</Trans>}
    />
  );
}
