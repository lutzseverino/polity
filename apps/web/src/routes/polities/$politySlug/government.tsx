import { msg } from "@lingui/core/macro";
import { Plural, Trans, useLingui } from "@lingui/react/macro";
import { createFileRoute, getRouteApi, notFound } from "@tanstack/react-router";

import { AppBadge } from "@/components/app/AppBadge";
import {
  AppCard,
  AppCardContent,
  AppCardDescription,
  AppCardHeader,
  AppCardTitle,
} from "@/components/app/AppCard";
import { AppText } from "@/components/app/AppText";
import {
  type PolityGovernment,
  polityGovernmentQueryOptions,
  polityReferenceQueryOptions,
  usePolityGovernment,
} from "@/domains/polity";
import { isResourceNotFoundError } from "@/lib/resource-not-found";

const polityRoute = getRouteApi("/polities/$politySlug");

export const Route = createFileRoute("/polities/$politySlug/government")({
  component: GovernmentRoute,
  loader: async ({ context, params }) => {
    try {
      const polity = await context.queryClient.ensureQueryData(
        polityReferenceQueryOptions({
          locale: context.getLocale(),
          polityReference: params.politySlug,
        }),
      );
      await context.queryClient.ensureQueryData(
        polityGovernmentQueryOptions({
          locale: context.getLocale(),
          polityId: polity.id,
        }),
      );
    } catch (error) {
      if (isResourceNotFoundError(error)) throw notFound();
      throw error;
    }
  },
  staticData: {
    shell: {
      label: msg`Government`,
      level: "workspace",
      target: { params: "politySlug", to: "/polities/$politySlug/government" },
    },
  },
});

type Government = PolityGovernment;

function readinessLabel(status: Government["readiness"]["status"]) {
  switch (status) {
    case "ready":
      return <Trans>Ready</Trans>;
    case "forming_offices":
      return <Trans>Forming offices</Trans>;
    case "provisional":
      return <Trans>Getting started</Trans>;
    case "blocked":
      return <Trans>Needs attention</Trans>;
    case "disbanded":
      return <Trans>Closed</Trans>;
  }
}

function healthLabel(status: Government["health"]["status"]) {
  switch (status) {
    case "healthy":
      return <Trans>Healthy</Trans>;
    case "degraded":
      return <Trans>Some paths are unavailable</Trans>;
    case "critical":
      return <Trans>Needs attention</Trans>;
  }
}

function institutionKindLabel(
  kind: Government["institutions"][number]["kind"],
) {
  switch (kind) {
    case "assembly":
      return <Trans>Assembly</Trans>;
    case "council":
      return <Trans>Council</Trans>;
    case "judiciary":
      return <Trans>Judiciary</Trans>;
  }
}

function electorateLabel(
  electorate: Government["procedures"][number]["electorate"],
) {
  return electorate === "active_members" ? (
    <Trans>All active members</Trans>
  ) : (
    <Trans>Current office holders</Trans>
  );
}

function thresholdLabel(
  threshold: Government["procedures"][number]["threshold"],
) {
  switch (threshold) {
    case "simple_majority_cast":
      return <Trans>Simple majority of votes cast</Trans>;
    case "majority_of_eligible":
      return <Trans>Majority of eligible members</Trans>;
    case "two_thirds_cast":
      return <Trans>Two thirds of votes cast</Trans>;
    case "two_thirds_eligible":
      return <Trans>Two thirds of eligible members</Trans>;
    case "office_election_result":
      return <Trans>Election result</Trans>;
  }
}

function GovernmentRoute() {
  const { i18n } = useLingui();
  const { polityId } = polityRoute.useLoaderData();
  const { data: government } = usePolityGovernment({
    locale: i18n.locale,
    polityId,
  });

  return (
    <div className="mx-auto max-w-4xl space-y-8">
      <section aria-labelledby="government-status-heading">
        <div className="mb-3">
          <AppText
            as="h2"
            id="government-status-heading"
            variant="sectionTitle"
          >
            <Trans>Government status</Trans>
          </AppText>
          <AppText className="mt-1" variant="supporting">
            <Trans>Whether this polity can make and carry out decisions.</Trans>
          </AppText>
        </div>
        <div className="grid gap-3 md:grid-cols-2">
          <AppCard size="sm">
            <AppCardHeader>
              <AppCardDescription>
                <Trans>Readiness</Trans>
              </AppCardDescription>
              <AppCardTitle>
                {readinessLabel(government.readiness.status)}
              </AppCardTitle>
            </AppCardHeader>
            <AppCardContent className="space-y-3">
              <AppText variant="supporting">
                {government.readiness.statusMessage}
              </AppText>
              <AppText variant="caption">
                <Plural
                  value={government.formation.activeMemberCount}
                  one="# active member"
                  other="# active members"
                />
                {" · "}
                <Plural
                  value={government.formation.standingMemberCount}
                  one="# in good standing"
                  other="# in good standing"
                />
              </AppText>
            </AppCardContent>
          </AppCard>

          <AppCard size="sm">
            <AppCardHeader>
              <AppCardDescription>
                <Trans>Constitutional health</Trans>
              </AppCardDescription>
              <AppCardTitle>
                {healthLabel(government.health.status)}
              </AppCardTitle>
            </AppCardHeader>
            <AppCardContent>
              <AppText variant="supporting">
                {government.health.statusMessage}
              </AppText>
            </AppCardContent>
          </AppCard>
        </div>
      </section>

      <section aria-labelledby="constitution-heading">
        <AppCard>
          <AppCardHeader>
            <div className="mb-1 flex flex-wrap items-center gap-2">
              <AppBadge variant="outline">
                <Trans>Version {government.constitution.version}</Trans>
              </AppBadge>
              <AppText as="span" variant="caption">
                <Trans>
                  Ratified {government.constitution.ratifiedAtLabel}
                </Trans>
              </AppText>
            </div>
            <AppCardTitle>
              <AppText as="h2" id="constitution-heading" variant="sectionTitle">
                {government.constitution.title}
              </AppText>
            </AppCardTitle>
          </AppCardHeader>
          <AppCardContent>
            <AppText variant="prose">{government.constitution.body}</AppText>
          </AppCardContent>
        </AppCard>
      </section>

      <section aria-labelledby="institutions-heading">
        <div className="mb-3">
          <AppText as="h2" id="institutions-heading" variant="sectionTitle">
            <Trans>Institutions</Trans>
          </AppText>
          <AppText className="mt-1" variant="supporting">
            <Trans>The bodies and offices entrusted with governing.</Trans>
          </AppText>
        </div>
        {government.institutions.length + government.offices.length > 0 ? (
          <div className="grid gap-3 md:grid-cols-2">
            {government.institutions.map((institution) => (
              <AppCard key={institution.id} size="sm">
                <AppCardHeader>
                  <AppCardDescription>
                    {institutionKindLabel(institution.kind)}
                  </AppCardDescription>
                  <AppCardTitle>{institution.name}</AppCardTitle>
                </AppCardHeader>
              </AppCard>
            ))}
            {government.offices.map((office) => (
              <AppCard key={office.id} size="sm">
                <AppCardHeader>
                  <AppCardDescription>
                    <Trans>Office</Trans>
                  </AppCardDescription>
                  <AppCardTitle>{office.name}</AppCardTitle>
                </AppCardHeader>
                <AppCardContent className="space-y-2">
                  <AppText variant="supporting">{office.description}</AppText>
                  <AppText variant="caption">
                    <Plural
                      value={office.seatCount}
                      one="# seat"
                      other="# seats"
                    />
                    {" · "}
                    <Plural
                      value={office.termLengthDays}
                      one="# day per term"
                      other="# days per term"
                    />
                  </AppText>
                </AppCardContent>
              </AppCard>
            ))}
          </div>
        ) : (
          <div className="rounded-xl border border-dashed px-4 py-5">
            <AppText variant="strong">
              <Trans>No institutions or offices yet</Trans>
            </AppText>
            <AppText className="mt-1" variant="caption">
              <Trans>They will appear here as this polity forms.</Trans>
            </AppText>
          </div>
        )}
      </section>

      <section aria-labelledby="procedures-heading">
        <div className="mb-3">
          <AppText as="h2" id="procedures-heading" variant="sectionTitle">
            <Trans>How decisions are made</Trans>
          </AppText>
          <AppText className="mt-1" variant="supporting">
            <Trans>
              Each kind of decision follows its constitutional procedure.
            </Trans>
          </AppText>
        </div>
        {government.procedures.length > 0 ? (
          <div className="grid gap-3 md:grid-cols-2">
            {government.procedures.map((procedure) => (
              <AppCard key={procedure.id} size="sm">
                <AppCardHeader>
                  <AppCardTitle>{procedure.name}</AppCardTitle>
                </AppCardHeader>
                <AppCardContent>
                  <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2">
                    <AppText as="dt" variant="captionStrong">
                      <Trans>Who votes</Trans>
                    </AppText>
                    <AppText as="dd" variant="supporting">
                      {electorateLabel(procedure.electorate)}
                    </AppText>
                    <AppText as="dt" variant="captionStrong">
                      <Trans>Passing rule</Trans>
                    </AppText>
                    <AppText as="dd" variant="supporting">
                      {thresholdLabel(procedure.threshold)}
                    </AppText>
                    <AppText as="dt" variant="captionStrong">
                      <Trans>Timing</Trans>
                    </AppText>
                    <AppText as="dd" variant="supporting">
                      <Trans>
                        {procedure.minimumNoticeHours} hours’ notice ·{" "}
                        {procedure.votingPeriodHours} hours to vote
                      </Trans>
                    </AppText>
                  </dl>
                </AppCardContent>
              </AppCard>
            ))}
          </div>
        ) : (
          <div className="rounded-xl border border-dashed px-4 py-5">
            <AppText variant="strong">
              <Trans>No decision procedures yet</Trans>
            </AppText>
            <AppText className="mt-1" variant="caption">
              <Trans>Constitutional procedures will appear here.</Trans>
            </AppText>
          </div>
        )}
      </section>
    </div>
  );
}
