import { useLingui } from "@lingui/react";
import { createFileRoute } from "@tanstack/react-router";

import { usePolityOptions } from "@/domains/polity";
import { ActionSetup } from "@/features/launch-action";

type ActionSearch = Readonly<{
  action?: string;
  polity?: string;
}>;

export const Route = createFileRoute("/actions/new")({
  component: ActionSetupRoute,
  validateSearch: (search): ActionSearch => ({
    action: typeof search.action === "string" ? search.action : undefined,
    polity: typeof search.polity === "string" ? search.polity : undefined,
  }),
});

function ActionSetupRoute() {
  const { i18n } = useLingui();
  const { data: polityOptions } = usePolityOptions({ locale: i18n.locale });
  const search = Route.useSearch();

  return (
    <ActionSetup
      actionId={search.action}
      polities={polityOptions}
      polityId={search.polity}
    />
  );
}
