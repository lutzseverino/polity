import { Trans, useLingui } from "@lingui/react/macro";
import { createFileRoute } from "@tanstack/react-router";

import { AppPageHeader } from "@/components/app/AppPageHeader";
import { usePolityOptions } from "@/domains/polity";
import { ActionLauncher } from "@/features/launch-action";

export const Route = createFileRoute("/home/")({ component: HomeRoute });

function HomeRoute() {
  const { i18n } = useLingui();
  const { data: polityOptions } = usePolityOptions({ locale: i18n.locale });

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <AppPageHeader
        description={
          <Trans>
            Start an official action now. Your cross-polity feed and
            followed-polity updates will grow around this workspace later.
          </Trans>
        }
        eyebrow={<Trans>Your Work Across Polities</Trans>}
        title={<Trans>Home</Trans>}
      />
      <ActionLauncher polities={polityOptions} variant="surface" />
    </div>
  );
}
