import { msg } from "@lingui/core/macro";
import { Trans } from "@lingui/react/macro";
import { createFileRoute } from "@tanstack/react-router";

import { AppPlannedDestination } from "@/components/app/AppPlannedDestination";

export const Route = createFileRoute("/polities/$polityId/government")({
  component: GovernmentRoute,
  staticData: {
    shell: {
      label: msg`Government`,
      level: "workspace",
      target: { params: "polityId", to: "/polities/$polityId/government" },
    },
  },
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
