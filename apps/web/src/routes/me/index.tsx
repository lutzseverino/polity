import { msg } from "@lingui/core/macro";
import { Trans } from "@lingui/react/macro";
import { createFileRoute } from "@tanstack/react-router";
import { AppPageHeader } from "@/components/app/AppPageHeader";
import { AppPageLayout } from "@/components/app/AppPageLayout";
import { AppPlannedDestination } from "@/components/app/AppPlannedDestination";

export const Route = createFileRoute("/me/")({
  component: MeRoute,
  staticData: {
    shell: {
      label: msg`Me`,
      level: "root",
      section: "me",
      target: { to: "/me" },
    },
  },
});

function MeRoute() {
  return (
    <AppPageLayout measure="focused">
      <AppPageHeader title={<Trans>Me</Trans>} />
      <AppPlannedDestination
        description={
          <Trans>
            Profile, account, language, accessibility, and appearance
            preferences will live here.
          </Trans>
        }
        title={<Trans>Account and Preferences</Trans>}
        titleId="planned-page-title"
      />
    </AppPageLayout>
  );
}
