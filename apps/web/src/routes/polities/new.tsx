import { Trans } from "@lingui/react/macro";
import { createFileRoute } from "@tanstack/react-router";

import { AppPlannedDestination } from "@/components/app/AppPlannedDestination";

export const Route = createFileRoute("/polities/new")({
  component: NewPolityRoute,
});

function NewPolityRoute() {
  return (
    <AppPlannedDestination
      className="max-w-3xl"
      description={
        <Trans>
          The founding flow is the next product slice after the core governing
          journey.
        </Trans>
      }
      label={<Trans>Planned polity destination</Trans>}
      title={<Trans>Found a polity</Trans>}
    />
  );
}
