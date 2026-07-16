import { msg } from "@lingui/core/macro";
import { Trans } from "@lingui/react/macro";
import { createFileRoute } from "@tanstack/react-router";
import { AppPageHeader } from "@/components/app/AppPageHeader";
import { AppPageLayout } from "@/components/app/AppPageLayout";
import { AppPlannedDestination } from "@/components/app/AppPlannedDestination";

export const Route = createFileRoute("/explore/")({
  component: ExploreRoute,
  staticData: {
    shell: {
      label: msg`Explore`,
      level: "root",
      section: "explore",
      target: { to: "/explore" },
    },
  },
});

function ExploreRoute() {
  return (
    <AppPageLayout measure="focused">
      <AppPageHeader title={<Trans>Explore</Trans>} />
      <AppPlannedDestination
        description={
          <Trans>
            Public polity discovery will be designed after the core governing
            journey is proven.
          </Trans>
        }
        title={<Trans>Public Polity Discovery</Trans>}
        titleId="planned-page-title"
      />
    </AppPageLayout>
  );
}
