import { msg } from "@lingui/core/macro";
import { Trans, useLingui } from "@lingui/react/macro";
import { createFileRoute, getRouteApi, notFound } from "@tanstack/react-router";

import { AppBadge } from "@/components/app/AppBadge";
import {
  AppCard,
  AppCardContent,
  AppCardFooter,
  AppCardHeader,
  AppCardTitle,
} from "@/components/app/AppCard";
import { AppLinkButton } from "@/components/app/AppLinkButton";
import { AppText } from "@/components/app/AppText";
import {
  polityRecordQueryOptions,
  polityReferenceQueryOptions,
  usePolityRecord,
} from "@/domains/polity";
import { isResourceNotFoundError } from "@/lib/resource-not-found";

const polityRoute = getRouteApi("/polities/$politySlug");

export const Route = createFileRoute("/polities/$politySlug/record")({
  component: RecordRoute,
  loader: async ({ context, params }) => {
    try {
      const polity = await context.queryClient.ensureQueryData(
        polityReferenceQueryOptions({
          locale: context.getLocale(),
          polityReference: params.politySlug,
        }),
      );
      await context.queryClient.ensureQueryData(
        polityRecordQueryOptions({
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
      label: msg`Official record`,
      level: "workspace",
      target: { params: "politySlug", to: "/polities/$politySlug/record" },
    },
  },
});

function RecordRoute() {
  const { i18n } = useLingui();
  const { politySlug } = Route.useParams();
  const { polityId } = polityRoute.useLoaderData();
  const { data: entries } = usePolityRecord({
    locale: i18n.locale,
    polityId,
  });

  return (
    <div className="mx-auto max-w-3xl space-y-5">
      <header>
        <AppText as="h2" variant="sectionTitle">
          <Trans>Official record</Trans>
        </AppText>
        <AppText className="mt-1" variant="supporting">
          <Trans>
            The permanent history of formal decisions and changes in this
            polity.
          </Trans>
        </AppText>
      </header>

      {entries.length > 0 ? (
        <ol className="space-y-3">
          {entries.map((entry) => (
            <li key={entry.id}>
              <AppCard size="sm">
                <AppCardHeader>
                  <div className="mb-1 flex flex-wrap items-center gap-2">
                    <AppBadge variant="outline">
                      <Trans>No. {entry.entryNumber}</Trans>
                    </AppBadge>
                    <AppText as="span" variant="caption">
                      {entry.occurredAtLabel}
                    </AppText>
                  </div>
                  <AppCardTitle>{entry.title}</AppCardTitle>
                </AppCardHeader>
                <AppCardContent className="space-y-3">
                  <AppText variant="body">{entry.body}</AppText>
                  <AppText variant="caption">
                    <Trans>Recorded by {entry.actorName}</Trans>
                    {" · "}
                    <Trans>Constitution v{entry.constitutionVersion}</Trans>
                  </AppText>
                </AppCardContent>
                {entry.motionId ? (
                  <AppCardFooter className="justify-end">
                    <AppLinkButton
                      params={{ motionId: entry.motionId, politySlug }}
                      size="sm"
                      to="/polities/$politySlug/motions/$motionId"
                      variant="ghost"
                    >
                      <Trans>View motion</Trans>
                    </AppLinkButton>
                  </AppCardFooter>
                ) : null}
              </AppCard>
            </li>
          ))}
        </ol>
      ) : (
        <div className="rounded-xl border border-dashed px-4 py-5">
          <AppText variant="strong">
            <Trans>No official activity yet</Trans>
          </AppText>
          <AppText className="mt-1" variant="caption">
            <Trans>Formal decisions and changes will appear here.</Trans>
          </AppText>
        </div>
      )}
    </div>
  );
}
