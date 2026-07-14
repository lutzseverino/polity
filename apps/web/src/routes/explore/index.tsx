import { Trans } from "@lingui/react/macro";
import { createFileRoute } from "@tanstack/react-router";

import { AppPlannedDestination } from "@/components/app/AppPlannedDestination";

export const Route = createFileRoute("/explore/")({
  component: ExploreRoute,
});

function ExploreRoute() {
  return (
    <AppPlannedDestination
      className="mx-auto max-w-3xl"
      description={
        <Trans>
          Public polity discovery will be designed after the core governing
          journey is proven.
        </Trans>
      }
      title={<Trans>Explore</Trans>}
      titleId="planned-page-title"
    />
  );
}
