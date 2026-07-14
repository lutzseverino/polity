import { msg } from "@lingui/core/macro";
import { Trans, useLingui } from "@lingui/react/macro";
import { createFileRoute, Link, notFound } from "@tanstack/react-router";
import { Info } from "lucide-react";
import {
  AppAlert,
  AppAlertDescription,
  AppAlertTitle,
} from "@/components/app/AppAlert";
import { AppBadge } from "@/components/app/AppBadge";
import {
  AppCard,
  AppCardContent,
  AppCardDescription,
  AppCardFooter,
  AppCardHeader,
  AppCardTitle,
} from "@/components/app/AppCard";
import { AppProgress, AppProgressLabel } from "@/components/app/AppProgress";
import { AppText } from "@/components/app/AppText";
import type { Motion } from "@/domains/motion";
import { polityMotionQueryOptions, usePolityMotion } from "@/domains/polity";
import { CastVotePanel } from "@/features/cast-vote";
import { NominationResponsePanel } from "@/features/respond-to-nomination";
import { isResourceNotFoundError } from "@/lib/resource-not-found";

export const Route = createFileRoute("/polities/$polityId/motions/$motionId")({
  component: MotionDetailRoute,
  loader: async ({ context, params }) => {
    try {
      const motion = await context.queryClient.ensureQueryData(
        polityMotionQueryOptions({
          locale: context.getLocale(),
          motionId: params.motionId,
          polityId: params.polityId,
        }),
      );

      return { shellLabel: motion.title };
    } catch (error) {
      if (isResourceNotFoundError(error)) {
        throw notFound();
      }

      throw error;
    }
  },
  staticData: {
    shell: {
      back: {
        label: msg`All motions`,
        target: { params: "polityId", to: "/polities/$polityId/motions" },
      },
      compactLabel: msg`Motion`,
      compactNavigation: "hidden",
      compactWorkspaceChrome: "hidden",
      level: "detail",
      showPrimaryAction: false,
    },
  },
});

function MotionResult({
  motion,
  polityId,
}: Readonly<{ motion: Motion; polityId: string }>) {
  if (!motion.result) {
    return null;
  }

  return (
    <AppCard>
      <AppCardHeader>
        <AppCardDescription>
          <Trans>Certified result</Trans>
        </AppCardDescription>
        <AppCardTitle>{motion.result.outcome}</AppCardTitle>
      </AppCardHeader>
      <AppCardContent>
        <dl className="grid grid-cols-2 gap-3">
          <div className="rounded-lg bg-muted p-3">
            <AppText as="dt" variant="supporting">
              <Trans>Yes</Trans>
            </AppText>
            <AppText as="dd" className="mt-1" variant="metric">
              {motion.result.yes}
            </AppText>
          </div>
          <div className="rounded-lg bg-muted p-3">
            <AppText as="dt" variant="supporting">
              <Trans>No</Trans>
            </AppText>
            <AppText as="dd" className="mt-1" variant="metric">
              {motion.result.no}
            </AppText>
          </div>
        </dl>
      </AppCardContent>
      <AppCardFooter>
        <Link
          className="text-sm font-medium hover:underline"
          params={{ polityId }}
          to="/polities/$polityId/record"
        >
          <Trans>Official record No. {motion.result.recordEntry}</Trans>
        </Link>
      </AppCardFooter>
    </AppCard>
  );
}

function MotionDecision({
  motion,
  polityId,
}: Readonly<{ motion: Motion; polityId: string }>) {
  if (motion.status !== "voting") {
    return <MotionResult motion={motion} polityId={polityId} />;
  }

  return motion.actionKind === "vote" ? (
    <CastVotePanel motion={motion} polityId={polityId} />
  ) : (
    <NominationResponsePanel motion={motion} polityId={polityId} />
  );
}

function MotionDetailRoute() {
  const { i18n, t } = useLingui();
  const { motionId, polityId } = Route.useParams();
  const { data: motion } = usePolityMotion({
    locale: i18n.locale,
    motionId,
    polityId,
  });
  const participationPercent = Math.round(
    (motion.participation.cast / motion.participation.eligible) * 100,
  );

  return (
    <div className="space-y-5">
      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_22rem] xl:items-start">
        <div className="min-w-0 space-y-5">
          <article>
            <div className="mb-3 flex flex-wrap items-center gap-2">
              <AppBadge
                variant={motion.status === "voting" ? "default" : "secondary"}
              >
                {motion.status === "voting" ? (
                  <Trans>Voting open</Trans>
                ) : (
                  (motion.result?.outcome ?? motion.status)
                )}
              </AppBadge>
              <AppBadge variant="outline">{motion.category}</AppBadge>
              <AppText as="span" variant="caption">
                {motion.status === "voting" ? (
                  <Trans>Closes {motion.closesAtLabel.toLowerCase()}</Trans>
                ) : (
                  <Trans>Closed {motion.closesAtLabel.toLowerCase()}</Trans>
                )}
              </AppText>
            </div>
            <AppText as="h1" variant="pageTitle">
              {motion.title}
            </AppText>
            <AppText className="mt-2" variant="supporting">
              <Trans>Introduced by {motion.introducedBy}</Trans>
            </AppText>
            <AppText className="mt-5 max-w-3xl" variant="prose">
              {motion.body}
            </AppText>
          </article>

          <MotionDecision motion={motion} polityId={polityId} />

          <AppAlert>
            <Info aria-hidden="true" />
            <AppAlertTitle>
              <Trans>Discussion is separate from government action</Trans>
            </AppAlertTitle>
            <AppAlertDescription>
              <Trans>
                Comments and reactions will appear here when the social layer is
                introduced. They will never change votes, eligibility, or the
                official result.
              </Trans>
            </AppAlertDescription>
          </AppAlert>
        </div>

        <aside className="space-y-4" aria-label={t`Motion procedure`}>
          <AppCard size="sm">
            <AppCardHeader>
              <AppCardTitle>
                <Trans>Participation</Trans>
              </AppCardTitle>
              <AppCardDescription>
                <Trans>
                  {motion.participation.cast} of {motion.participation.eligible}{" "}
                  eligible members have participated.
                </Trans>
              </AppCardDescription>
            </AppCardHeader>
            <AppCardContent>
              <AppProgress value={participationPercent}>
                <AppProgressLabel>
                  <Trans>Quorum</Trans>
                </AppProgressLabel>
                <AppText
                  as="span"
                  className="ml-auto tabular-nums"
                  variant="supporting"
                >
                  {motion.participation.quorumMet ? (
                    <Trans>Met</Trans>
                  ) : (
                    <Trans>{motion.participation.quorumRequired} needed</Trans>
                  )}
                </AppText>
              </AppProgress>
            </AppCardContent>
          </AppCard>

          <AppCard size="sm">
            <AppCardHeader>
              <AppCardTitle>
                <Trans>How this decision works</Trans>
              </AppCardTitle>
              <AppCardDescription>{motion.procedure.name}</AppCardDescription>
            </AppCardHeader>
            <AppCardContent>
              <dl className="space-y-3">
                <div>
                  <AppText as="dt" variant="supporting">
                    <Trans>Who participates</Trans>
                  </AppText>
                  <AppText as="dd" className="mt-1" variant="strong">
                    {motion.procedure.electorate}
                  </AppText>
                </div>
                <div>
                  <AppText as="dt" variant="supporting">
                    <Trans>Passing rule</Trans>
                  </AppText>
                  <AppText as="dd" className="mt-1" variant="strong">
                    {motion.procedure.threshold}
                  </AppText>
                </div>
                <div>
                  <AppText as="dt" variant="supporting">
                    <Trans>Minimum notice</Trans>
                  </AppText>
                  <AppText as="dd" className="mt-1" variant="strong">
                    {motion.procedure.notice}
                  </AppText>
                </div>
              </dl>
            </AppCardContent>
          </AppCard>
        </aside>
      </div>
    </div>
  );
}
