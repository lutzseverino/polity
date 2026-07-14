import { Trans, useLingui } from "@lingui/react/macro";
import { createFileRoute, getRouteApi, Link } from "@tanstack/react-router";
import {
  ArrowRight,
  CheckCircle2,
  CircleAlert,
  Clock3,
  Users,
} from "lucide-react";

import { AppBadge } from "@/components/app/AppBadge";
import {
  AppCard,
  AppCardAction,
  AppCardContent,
  AppCardDescription,
  AppCardHeader,
  AppCardTitle,
} from "@/components/app/AppCard";
import { AppText } from "@/components/app/AppText";
import { MotionSummaryCard } from "@/domains/motion";
import { usePolities, usePolity } from "@/domains/polity";
import { ActionLauncher } from "@/features/launch-action";

const polityRoute = getRouteApi("/polities/$polityId");

export const Route = createFileRoute("/polities/$polityId/")({
  component: PolityOverviewRoute,
});

function PolityOverviewRoute() {
  const { i18n, t } = useLingui();
  const { polityId } = polityRoute.useParams();
  const { data: polities } = usePolities({ locale: i18n.locale });
  const { data: polity } = usePolity({
    locale: i18n.locale,
    polityId,
  });
  const openMotions = polity.motions.filter(
    (motion) => motion.status === "voting",
  );

  return (
    <div className="space-y-6">
      <div className="md:hidden">
        <ActionLauncher
          defaultPolityId={polity.id}
          polities={polities.map(({ id, name }) => ({ id, name }))}
          variant="surface"
        />
      </div>
      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_20rem]">
        <div className="min-w-0 space-y-6">
          <section aria-labelledby="attention-heading">
            <div className="mb-3 flex items-center justify-between gap-3">
              <div>
                <AppText as="h2" id="attention-heading" variant="sectionTitle">
                  <Trans>Needs your attention</Trans>
                </AppText>
                <AppText className="mt-1" variant="supporting">
                  <Trans>The actions only you can complete right now.</Trans>
                </AppText>
              </div>
              {polity.attention.length > 0 ? (
                <AppBadge>{polity.attention.length}</AppBadge>
              ) : null}
            </div>

            {polity.attention.length > 0 ? (
              <div className="space-y-3">
                {polity.attention.map((item) => (
                  <AppCard key={item.id} size="sm">
                    <AppCardHeader>
                      <div className="mb-1 flex items-center gap-2 text-muted-foreground">
                        {item.kind === "vote" ? (
                          <CircleAlert aria-hidden="true" className="size-4" />
                        ) : item.kind === "candidacy" ? (
                          <Users aria-hidden="true" className="size-4" />
                        ) : (
                          <CheckCircle2 aria-hidden="true" className="size-4" />
                        )}
                        <AppText as="span" variant="caption">
                          {item.dueLabel}
                        </AppText>
                      </div>
                      <AppCardTitle>{item.title}</AppCardTitle>
                      <AppCardDescription>
                        {item.description}
                      </AppCardDescription>
                      <AppCardAction>
                        <Link
                          aria-label={t`Open ${item.title}`}
                          className="inline-flex size-8 items-center justify-center rounded-lg text-muted-foreground hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                          params={
                            item.target.kind === "motion"
                              ? {
                                  motionId: item.target.motionId,
                                  polityId: polity.id,
                                }
                              : { polityId: polity.id }
                          }
                          to={
                            item.target.kind === "motion"
                              ? "/polities/$polityId/motions/$motionId"
                              : "/polities/$polityId"
                          }
                        >
                          <ArrowRight aria-hidden="true" className="size-4" />
                        </Link>
                      </AppCardAction>
                    </AppCardHeader>
                  </AppCard>
                ))}
              </div>
            ) : (
              <AppCard size="sm">
                <AppCardContent className="flex items-center gap-3 text-muted-foreground">
                  <CheckCircle2 aria-hidden="true" className="size-5" />
                  <AppText as="span" variant="supporting">
                    <Trans>Nothing needs your attention right now.</Trans>
                  </AppText>
                </AppCardContent>
              </AppCard>
            )}
          </section>

          <section aria-labelledby="active-motions-heading">
            <div className="mb-3 flex items-end justify-between gap-3">
              <div>
                <AppText
                  as="h2"
                  id="active-motions-heading"
                  variant="sectionTitle"
                >
                  <Trans>Active motions</Trans>
                </AppText>
                <AppText className="mt-1" variant="supporting">
                  <Trans>Proceedings currently open in this polity.</Trans>
                </AppText>
              </div>
              <Link
                className="text-sm font-medium hover:underline focus-visible:rounded focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                params={{ polityId: polity.id }}
                to="/polities/$polityId/motions"
              >
                <Trans>View all</Trans>
              </Link>
            </div>
            {openMotions.length > 0 ? (
              <div className="grid gap-3 lg:grid-cols-2">
                {openMotions.map((motion) => (
                  <MotionSummaryCard
                    key={motion.id}
                    motion={motion}
                    polityId={polity.id}
                  />
                ))}
              </div>
            ) : (
              <AppCard size="sm">
                <AppCardContent>
                  <AppText variant="supporting">
                    <Trans>There are no active motions.</Trans>
                  </AppText>
                </AppCardContent>
              </AppCard>
            )}
          </section>

          <section aria-labelledby="recent-activity-heading">
            <div className="mb-3">
              <AppText
                as="h2"
                id="recent-activity-heading"
                variant="sectionTitle"
              >
                <Trans>Recent official activity</Trans>
              </AppText>
              <AppText className="mt-1" variant="supporting">
                <Trans>
                  Government events, separate from social discussion.
                </Trans>
              </AppText>
            </div>
            <AppCard size="sm">
              <AppCardContent>
                {polity.recentActivity.length > 0 ? (
                  <ol className="divide-y">
                    {polity.recentActivity.map((activity) => (
                      <li
                        className="flex gap-3 py-3 first:pt-0 last:pb-0"
                        key={activity.id}
                      >
                        <Clock3
                          aria-hidden="true"
                          className="mt-0.5 size-4 shrink-0 text-muted-foreground"
                        />
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
                  <AppText variant="supporting">
                    <Trans>No official activity yet.</Trans>
                  </AppText>
                )}
              </AppCardContent>
            </AppCard>
          </section>
        </div>

        <aside className="space-y-4" aria-label={t`Polity status`}>
          <AppCard size="sm">
            <AppCardHeader>
              <AppCardTitle>
                <Trans>Government status</Trans>
              </AppCardTitle>
              <AppCardDescription>{polity.readinessMessage}</AppCardDescription>
            </AppCardHeader>
            <AppCardContent>
              <dl className="space-y-3">
                <div className="flex justify-between gap-4">
                  <AppText as="dt" variant="supporting">
                    <Trans>Readiness</Trans>
                  </AppText>
                  <AppText as="dd" className="capitalize" variant="strong">
                    {polity.readiness === "ready" ? (
                      <Trans>Ready</Trans>
                    ) : (
                      <Trans>Forming</Trans>
                    )}
                  </AppText>
                </div>
                <div className="flex justify-between gap-4">
                  <AppText as="dt" variant="supporting">
                    <Trans>Members</Trans>
                  </AppText>
                  <AppText as="dd" variant="strong">
                    {polity.memberCount}
                  </AppText>
                </div>
                <div className="flex justify-between gap-4">
                  <AppText as="dt" variant="supporting">
                    <Trans>Constitution</Trans>
                  </AppText>
                  <AppText as="dd" variant="strong">
                    <Trans>Version {polity.constitutionVersion}</Trans>
                  </AppText>
                </div>
              </dl>
            </AppCardContent>
          </AppCard>

          <AppCard size="sm">
            <AppCardHeader>
              <AppCardTitle>
                <Trans>Your standing</Trans>
              </AppCardTitle>
            </AppCardHeader>
            <AppCardContent>
              <AppText variant="strong">{polity.role}</AppText>
              <AppText className="mt-1" variant="supporting">
                <Trans>
                  Available actions are shown when the constitution and current
                  proceeding allow them.
                </Trans>
              </AppText>
            </AppCardContent>
          </AppCard>
        </aside>
      </div>
    </div>
  );
}
