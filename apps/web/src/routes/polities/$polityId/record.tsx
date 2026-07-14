import { msg } from "@lingui/core/macro";
import { Trans } from "@lingui/react/macro";
import { createFileRoute } from "@tanstack/react-router";

import { AppPlannedDestination } from "@/components/app/AppPlannedDestination";

export const Route = createFileRoute("/polities/$polityId/record")({
  component: RecordRoute,
  staticData: {
    shell: {
      label: msg`Official record`,
      level: "workspace",
      target: { params: "polityId", to: "/polities/$polityId/record" },
    },
  },
});

function RecordRoute() {
  return (
    <AppPlannedDestination
      className="max-w-3xl"
      description={
        <Trans>
          The permanent, numbered official record will be the next read-focused
          polity destination.
        </Trans>
      }
      label={<Trans>Planned polity destination</Trans>}
      title={<Trans>Official record</Trans>}
    />
  );
}
