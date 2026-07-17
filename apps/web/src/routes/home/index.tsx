import { msg } from "@lingui/core/macro";
import { Trans, useLingui } from "@lingui/react/macro";
import { createFileRoute } from "@tanstack/react-router";
import type { ReactNode } from "react";

import { AppLinkButton } from "@/components/app/AppLinkButton";
import {
  AppLinkSurface,
  AppLinkSurfaceIndicator,
} from "@/components/app/AppLinkSurface";
import { AppPageHeader } from "@/components/app/AppPageHeader";
import { AppPageLayout } from "@/components/app/AppPageLayout";
import { usePolityOptions } from "@/domains/polity";
import {
  ActionLauncher,
  type ActionLauncherActionLinkProps,
  type ActionLauncherEmptyActionLinkProps,
} from "@/features/launch-action";

function renderActionLink({
  actionId,
  children,
  className,
  onSelect,
  polityId,
}: ActionLauncherActionLinkProps) {
  return (
    <AppLinkSurface
      className={className}
      onClick={onSelect}
      search={{ action: actionId, polity: polityId }}
      to="/actions/new"
    >
      {children}
      <AppLinkSurfaceIndicator />
    </AppLinkSurface>
  );
}

function renderEmptyActionLink({
  children,
  kind,
}: ActionLauncherEmptyActionLinkProps): ReactNode {
  return kind === "explore-polities" ? (
    <AppLinkButton to="/explore" variant="outline">
      {children}
    </AppLinkButton>
  ) : (
    <AppLinkButton to="/polities/new">{children}</AppLinkButton>
  );
}

export const Route = createFileRoute("/home/")({
  component: HomeRoute,
  staticData: {
    shell: {
      label: msg`Home`,
      level: "root",
      section: "home",
      target: { to: "/home" },
    },
  },
});

function HomeRoute() {
  const { i18n } = useLingui();
  const { data: polityOptions } = usePolityOptions({ locale: i18n.locale });

  return (
    <AppPageLayout measure="standard">
      <AppPageHeader
        description={
          <Trans>
            Start an official action now. Your cross-polity feed and
            followed-polity updates will grow around this workspace later.
          </Trans>
        }
        title={<Trans>Your Work Across Polities</Trans>}
      />
      <ActionLauncher
        polities={polityOptions}
        renderActionLink={renderActionLink}
        renderEmptyActionLink={renderEmptyActionLink}
        variant="surface"
      />
    </AppPageLayout>
  );
}
