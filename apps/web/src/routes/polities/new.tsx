import { msg } from "@lingui/core/macro";
import { Trans } from "@lingui/react/macro";
import { createFileRoute } from "@tanstack/react-router";

import { AppPlannedDestination } from "@/components/app/AppPlannedDestination";

export const Route = createFileRoute("/polities/new")({
  component: NewPolityRoute,
  staticData: {
    shell: {
      back: { label: msg`Back to Polities`, target: { to: "/polities" } },
      compactNavigation: "hidden",
      label: msg`Found a polity`,
      level: "task",
      section: "polities",
      showPrimaryAction: false,
    },
  },
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
