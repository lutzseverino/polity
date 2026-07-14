import { Plural, Trans, useLingui } from "@lingui/react/macro";
import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowRight, Landmark, Plus } from "lucide-react";

import { AppBadge } from "@/components/app/AppBadge";
import {
  AppCard,
  AppCardContent,
  AppCardDescription,
  AppCardHeader,
  AppCardTitle,
} from "@/components/app/AppCard";
import { AppPageHeader } from "@/components/app/AppPageHeader";
import { AppText } from "@/components/app/AppText";
import { invitationsQueryOptions, useInvitations } from "@/domains/membership";
import {
  PolityCard,
  politiesQueryOptions,
  ReadinessBadge,
  usePolities,
} from "@/domains/polity";

const invitationPreviewLimit = 3;
type PolityDirectorySearch = Readonly<{
  invitations?: "all";
}>;

export const Route = createFileRoute("/polities/")({
  component: PolityDirectoryRoute,
  loader: ({ context }) => {
    const locale = context.getLocale();

    return Promise.all([
      context.queryClient.ensureQueryData(invitationsQueryOptions({ locale })),
      context.queryClient.ensureQueryData(politiesQueryOptions({ locale })),
    ]);
  },
  validateSearch: (search): PolityDirectorySearch => ({
    invitations: search.invitations === "all" ? "all" : undefined,
  }),
});

function PolityDirectoryRoute() {
  const { i18n, t } = useLingui();
  const { data: invitations } = useInvitations({ locale: i18n.locale });
  const { data: polities } = usePolities({ locale: i18n.locale });
  const { invitations: invitationVisibility } = Route.useSearch();
  const showAllInvitations = invitationVisibility === "all";
  const previewInvitations = showAllInvitations
    ? invitations
    : invitations.slice(0, invitationPreviewLimit);
  const remainingInvitationCount = Math.max(
    0,
    invitations.length - previewInvitations.length,
  );

  return (
    <div className="space-y-4">
      <AppPageHeader
        description={
          <Trans>
            See what needs you, enter a polity, and pick up where your group
            left off.
          </Trans>
        }
        eyebrow={<Trans>Your Government Spaces</Trans>}
        title={<Trans>Your Polities</Trans>}
      />

      <section aria-label={t`Invitations`} className="space-y-4">
        <div className="flex items-center gap-4">
          <div
            aria-hidden="true"
            className="h-px flex-1 bg-linear-to-r from-transparent via-border to-transparent"
          />
          {remainingInvitationCount > 0 ? (
            <Link
              className="hidden shrink-0 text-sm font-medium hover:underline focus-visible:rounded focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50 sm:inline-flex"
              search={{ invitations: "all" }}
              to="/polities"
            >
              <Plural
                value={remainingInvitationCount}
                one="View # More Invitation"
                other="View # More Invitations"
              />
            </Link>
          ) : null}
        </div>

        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {previewInvitations.map((invitation) => (
            <Link
              className="group min-w-0 rounded-xl focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
              key={invitation.id}
              params={{ invitationId: invitation.id }}
              to="/polities/invitations/$invitationId"
            >
              <AppCard className="h-full min-w-0 transition-colors group-hover:bg-muted/40">
                <AppCardHeader>
                  <div className="mb-2 flex items-center justify-between gap-3">
                    <AppBadge variant="outline">
                      <Trans>Invitation</Trans>
                    </AppBadge>
                    <AppText as="span" variant="caption">
                      {invitation.receivedLabel}
                    </AppText>
                  </div>
                  <AppCardTitle className="break-words">
                    {invitation.polityName}
                  </AppCardTitle>
                  <AppCardDescription>
                    <Trans>
                      {invitation.invitedBy} invited you to become a citizen.
                    </Trans>
                  </AppCardDescription>
                </AppCardHeader>
              </AppCard>
            </Link>
          ))}
        </div>

        {remainingInvitationCount > 0 ? (
          <Link
            className="mt-3 inline-flex text-sm font-medium hover:underline focus-visible:rounded focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50 sm:hidden"
            search={{ invitations: "all" }}
            to="/polities"
          >
            <Plural
              value={remainingInvitationCount}
              one="View # More Invitation"
              other="View # More Invitations"
            />
          </Link>
        ) : null}
      </section>

      <section aria-label={t`Your Polities`} className="space-y-4">
        <div
          aria-hidden="true"
          className="h-px bg-linear-to-r from-transparent via-border to-transparent"
        />
        <div className="grid gap-4 lg:grid-cols-2 xl:grid-cols-3">
          <Link
            className="group rounded-xl focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            to="/polities/new"
          >
            <AppCard className="h-full border border-dashed bg-transparent ring-0 transition-colors group-hover:border-foreground/30 group-hover:bg-muted/50">
              <AppCardContent className="flex h-full min-h-44 flex-col items-center justify-center py-6 text-center">
                <span className="mb-3 flex size-10 items-center justify-center rounded-full border border-dashed">
                  <Plus aria-hidden="true" className="size-5" />
                </span>
                <AppText variant="subsectionTitle">
                  <Trans>Found a Polity</Trans>
                </AppText>
                <AppText className="mt-1 max-w-56" variant="supporting">
                  <Trans>
                    Create a new space and establish how it will govern itself.
                  </Trans>
                </AppText>
              </AppCardContent>
            </AppCard>
          </Link>

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
                <PolityCard.Content className="flex flex-1 flex-col justify-between gap-4 space-y-0">
                  <AppText variant="supporting">
                    {polity.readinessMessage}
                  </AppText>
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
