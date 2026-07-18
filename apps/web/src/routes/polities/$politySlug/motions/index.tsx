import { msg } from "@lingui/core/macro";
import { Trans, useLingui } from "@lingui/react/macro";
import { createFileRoute, getRouteApi } from "@tanstack/react-router";
import type { ReactNode } from "react";

import { AppCard, AppCardContent } from "@/components/app/AppCard";
import {
  AppLinkSurface,
  AppLinkSurfaceIndicator,
} from "@/components/app/AppLinkSurface";
import { AppText } from "@/components/app/AppText";
import { type Motion, MotionSummary } from "@/domains/motion";
import { usePolity } from "@/domains/polity";

const polityRoute = getRouteApi("/polities/$politySlug");

type MotionSectionProps = Readonly<{
  emptyMessage: ReactNode;
  headingId: string;
  motions: readonly Motion[];
  politySlug: string;
  title: ReactNode;
}>;

function MotionSection({
  emptyMessage,
  headingId,
  motions,
  politySlug,
  title,
}: MotionSectionProps) {
  return (
    <section aria-labelledby={headingId}>
      <AppText
        as="h3"
        className="mb-3"
        id={headingId}
        variant="subsectionTitle"
      >
        {title}
      </AppText>
      {motions.length > 0 ? (
        <div className="grid gap-3 lg:grid-cols-2">
          {motions.map((motion) => (
            <AppLinkSurface
              className="h-full"
              key={motion.id}
              params={{ motionId: motion.id, politySlug }}
              to="/polities/$politySlug/motions/$motionId"
            >
              <MotionSummary
                action={<AppLinkSurfaceIndicator />}
                motion={motion}
              />
            </AppLinkSurface>
          ))}
        </div>
      ) : (
        <AppCard size="sm">
          <AppCardContent>
            <AppText variant="supporting">{emptyMessage}</AppText>
          </AppCardContent>
        </AppCard>
      )}
    </section>
  );
}

export const Route = createFileRoute("/polities/$politySlug/motions/")({
  component: MotionListRoute,
  staticData: {
    shell: {
      label: msg`Motions`,
      level: "workspace",
      target: { params: "politySlug", to: "/polities/$politySlug/motions" },
    },
  },
});

function MotionListRoute() {
  const { i18n } = useLingui();
  const { politySlug } = polityRoute.useParams();
  const { polityId } = polityRoute.useLoaderData();
  const { data: polity } = usePolity({
    locale: i18n.locale,
    polityId,
  });
  const activeMotions = polity.motions.filter(
    (motion) => motion.status === "voting",
  );
  const completedMotions = polity.motions.filter(
    (motion) => motion.status !== "voting",
  );

  return (
    <div className="space-y-8">
      <header>
        <AppText as="h2" variant="contentTitle">
          <Trans>Motions</Trans>
        </AppText>
        <AppText className="mt-1 max-w-2xl" variant="supporting">
          <Trans>
            Review active proceedings, cast eligible votes, and find certified
            results.
          </Trans>
        </AppText>
      </header>

      <MotionSection
        emptyMessage={<Trans>There are no open motions.</Trans>}
        headingId="open-motions-heading"
        motions={activeMotions}
        politySlug={politySlug}
        title={<Trans>Open now</Trans>}
      />

      <MotionSection
        emptyMessage={<Trans>No motions have been completed yet.</Trans>}
        headingId="completed-motions-heading"
        motions={completedMotions}
        politySlug={politySlug}
        title={<Trans>Completed</Trans>}
      />
    </div>
  );
}
