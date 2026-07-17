import { Trans, useLingui } from "@lingui/react/macro";
import { createFileRoute, getRouteApi } from "@tanstack/react-router";
import { CheckCircle2, CircleAlert, Clock3, Users } from "lucide-react";

import {
  AppCard,
  AppCardAction,
  AppCardDescription,
  AppCardHeader,
  AppCardTitle,
} from "@/components/app/AppCard";
import { AppLinkButton } from "@/components/app/AppLinkButton";
import {
  AppLinkSurface,
  AppLinkSurfaceIndicator,
} from "@/components/app/AppLinkSurface";
import { AppText } from "@/components/app/AppText";
import type { Polity } from "@/domains/polity";
import { usePolity, usePolityOptions } from "@/domains/polity";
import {
  ActionLauncher,
  type ActionLauncherActionLinkProps,
  type ActionLauncherEmptyActionLinkProps,
} from "@/features/launch-action";

const polityRoute = getRouteApi("/polities/$polityId");

export const Route = createFileRoute("/polities/$polityId/")({
  component: PolityOverviewRoute,
  staticData: {
    shell: {
      showPrimaryAction: false,
    },
  },
});

type AttentionItem = Polity["attention"][number];

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
}: ActionLauncherEmptyActionLinkProps) {
  return kind === "explore-polities" ? (
    <AppLinkButton to="/explore" variant="outline">
      {children}
    </AppLinkButton>
  ) : (
    <AppLinkButton to="/polities/new">{children}</AppLinkButton>
  );
}

function AttentionCard({
  item,
  polityId,
}: Readonly<{ item: AttentionItem; polityId: string }>) {
  const Icon =
    item.kind === "vote"
      ? CircleAlert
      : item.kind === "candidacy"
        ? Users
        : CheckCircle2;
  const content = (
    <AppCard
      className="transition-[background-color,box-shadow] group-hover/link-surface:bg-muted/40 group-hover/link-surface:ring-foreground/20"
      size="sm"
    >
      <AppCardHeader>
        <div className="mb-1 flex items-center gap-2 text-muted-foreground">
          <Icon aria-hidden="true" className="size-4" />
          <AppText as="span" variant="caption">
            {item.dueLabel}
          </AppText>
        </div>
        <AppCardTitle>{item.title}</AppCardTitle>
        <AppCardDescription>{item.description}</AppCardDescription>
        <AppCardAction className="self-center">
          <AppLinkSurfaceIndicator />
        </AppCardAction>
      </AppCardHeader>
    </AppCard>
  );

  if (item.target.kind === "action") {
    return (
      <AppLinkSurface
        search={{ action: item.target.actionId, polity: polityId }}
        to="/actions/new"
      >
        {content}
      </AppLinkSurface>
    );
  }

  return (
    <AppLinkSurface
      params={{ motionId: item.target.motionId, polityId }}
      to="/polities/$polityId/motions/$motionId"
    >
      {content}
    </AppLinkSurface>
  );
}

function PolityOverviewRoute() {
  const { i18n } = useLingui();
  const { polityId } = polityRoute.useParams();
  const { data: polities } = usePolityOptions({ locale: i18n.locale });
  const { data: polity } = usePolity({
    locale: i18n.locale,
    polityId,
  });

  return (
    <div className="mx-auto max-w-3xl space-y-8">
      <ActionLauncher
        defaultPolityId={polity.id}
        polities={polities}
        renderActionLink={renderActionLink}
        renderEmptyActionLink={renderEmptyActionLink}
        triggerPresentation="prompt"
      />

      <section aria-labelledby="for-you-heading">
        <div className="mb-3">
          <AppText as="h2" id="for-you-heading" variant="sectionTitle">
            <Trans>For you</Trans>
          </AppText>
          <AppText className="mt-1" variant="supporting">
            <Trans>Decisions and requests waiting on you.</Trans>
          </AppText>
        </div>

        {polity.attention.length > 0 ? (
          <div className="space-y-3">
            {polity.attention.map((item) => (
              <AttentionCard item={item} key={item.id} polityId={polity.id} />
            ))}
          </div>
        ) : (
          <div className="flex items-start gap-3 rounded-xl border border-dashed px-4 py-4">
            <CheckCircle2
              aria-hidden="true"
              className="mt-0.5 size-5 shrink-0 text-muted-foreground"
            />
            <div>
              <AppText variant="strong">
                <Trans>You’re all caught up</Trans>
              </AppText>
              <AppText className="mt-0.5" variant="caption">
                <Trans>New decisions and requests will appear here.</Trans>
              </AppText>
            </div>
          </div>
        )}
      </section>

      <section aria-labelledby="latest-heading">
        <div className="mb-3">
          <AppText as="h2" id="latest-heading" variant="sectionTitle">
            <Trans>Latest</Trans>
          </AppText>
          <AppText className="mt-1" variant="supporting">
            <Trans>Formal changes and decisions from this polity.</Trans>
          </AppText>
        </div>

        {polity.recentActivity.length > 0 ? (
          <ol className="divide-y border-y">
            {polity.recentActivity.map((activity) => (
              <li className="flex items-start gap-3 py-4" key={activity.id}>
                <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-muted text-muted-foreground">
                  <Clock3 aria-hidden="true" className="size-4" />
                </span>
                <div className="min-w-0 flex-1">
                  <AppText variant="strong">{activity.title}</AppText>
                  <AppText className="mt-1" variant="caption">
                    {activity.label} · {activity.timeLabel}
                  </AppText>
                </div>
              </li>
            ))}
          </ol>
        ) : (
          <div className="rounded-xl border border-dashed px-4 py-5">
            <AppText variant="strong">
              <Trans>It’s quiet here for now</Trans>
            </AppText>
            <AppText className="mt-1" variant="caption">
              <Trans>Official activity will collect here as it happens.</Trans>
            </AppText>
          </div>
        )}
      </section>
    </div>
  );
}
