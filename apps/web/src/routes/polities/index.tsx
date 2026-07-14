import { msg } from "@lingui/core/macro";
import { Plural, Trans, useLingui } from "@lingui/react/macro";
import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowRight, Landmark, Plus } from "lucide-react";

import { AppBadge } from "@/components/app/AppBadge";
import {
  AppEmpty,
  AppEmptyDescription,
  AppEmptyHeader,
  AppEmptyMedia,
  AppEmptyTitle,
} from "@/components/app/AppEmpty";
import { AppPageHeader } from "@/components/app/AppPageHeader";
import { AppSeparator } from "@/components/app/AppSeparator";
import { AppText } from "@/components/app/AppText";
import {
  PolityCard,
  politiesQueryOptions,
  ReadinessBadge,
  usePolities,
} from "@/domains/polity";

export const Route = createFileRoute("/polities/")({
  component: PolityDirectoryRoute,
  loader: ({ context }) => {
    const locale = context.getLocale();

    return context.queryClient.ensureQueryData(
      politiesQueryOptions({ locale }),
    );
  },
  staticData: {
    shell: {
      label: msg`Polities`,
      level: "root",
      section: "polities",
      target: { to: "/polities" },
    },
  },
});

function PolityDirectoryRoute() {
  const { i18n, t } = useLingui();
  const { data: polities } = usePolities({ locale: i18n.locale });

  return (
    <div>
      <AppPageHeader
        compactVisibility="hidden"
        title={<Trans>Polities</Trans>}
      />

      <section aria-label={t`Polities`} className="space-y-4 md:pt-6">
        <AppSeparator
          aria-hidden="true"
          className="hidden md:block"
          variant="gradient"
        />
        <div className="grid gap-4 lg:grid-cols-2 xl:grid-cols-3">
          <Link
            className="group h-full rounded-xl focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            to="/polities/new"
          >
            <AppEmpty className="h-full flex-row justify-start gap-3 border bg-transparent p-4 text-left transition-colors group-hover:border-foreground/30 group-hover:bg-muted/50">
              <AppEmptyMedia
                className="mb-0 size-10 rounded-full border border-dashed bg-transparent"
                variant="icon"
              >
                <Plus aria-hidden="true" className="size-5" />
              </AppEmptyMedia>
              <AppEmptyHeader className="min-w-0 max-w-none items-start gap-1 text-left">
                <AppEmptyTitle className="text-base font-semibold">
                  <Trans>Found a Polity</Trans>
                </AppEmptyTitle>
                <AppEmptyDescription>
                  <Trans>
                    Create a new space and establish how it will govern itself.
                  </Trans>
                </AppEmptyDescription>
              </AppEmptyHeader>
            </AppEmpty>
          </Link>

          {polities.length > 0 ? (
            <AppSeparator
              aria-hidden="true"
              className="md:hidden"
              variant="gradient"
            />
          ) : null}

          {polities.map((polity) => (
            <Link
              className="group min-w-0 rounded-xl focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
              key={polity.id}
              params={{ polityId: polity.id }}
              to="/polities/$polityId"
            >
              <PolityCard>
                <PolityCard.Header
                  badges={
                    <>
                      <ReadinessBadge readiness={polity.readiness} />
                      <AppBadge variant="outline">
                        {polity.visibility === "public" ? (
                          <Trans>Public</Trans>
                        ) : (
                          <Trans>Private</Trans>
                        )}
                      </AppBadge>
                    </>
                  }
                >
                  <PolityCard.Identity
                    action={
                      <ArrowRight
                        aria-hidden="true"
                        className="mt-1 size-4 shrink-0 text-muted-foreground group-hover:text-foreground"
                      />
                    }
                    icon={<Landmark aria-hidden="true" className="size-4" />}
                  >
                    <PolityCard.Title className="break-words">
                      {polity.name}
                    </PolityCard.Title>
                    <PolityCard.Description>
                      {polity.role}
                    </PolityCard.Description>
                  </PolityCard.Identity>
                </PolityCard.Header>
                <PolityCard.Content className="flex flex-1 flex-col justify-end">
                  <PolityCard.Meta>
                    <AppText as="span">
                      <Plural
                        value={polity.memberCount}
                        one="# Member"
                        other="# Members"
                      />
                    </AppText>
                    {polity.attention.length > 0 ? (
                      <AppText as="span">
                        <Plural
                          value={polity.attention.length}
                          one="# Item Needs You"
                          other="# Items Need You"
                        />
                      </AppText>
                    ) : null}
                  </PolityCard.Meta>
                </PolityCard.Content>
              </PolityCard>
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
}
