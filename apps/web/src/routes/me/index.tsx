import { Trans } from "@lingui/react/macro";
import { createFileRoute } from "@tanstack/react-router";

import { AppPlannedDestination } from "@/components/app/AppPlannedDestination";

export const Route = createFileRoute("/me/")({
  component: MeRoute,
});

function MeRoute() {
  return (
    <AppPlannedDestination
      className="mx-auto max-w-3xl"
      description={
        <Trans>
          Profile, account, language, accessibility, and appearance preferences
          will live here.
        </Trans>
      }
      title={<Trans>Me</Trans>}
      titleId="planned-page-title"
    />
  );
}
