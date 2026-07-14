import { Trans, useLingui } from "@lingui/react/macro";
import { createFileRoute, notFound } from "@tanstack/react-router";

import { AppBackLink } from "@/components/app/AppBackLink";
import { AppLinkButton } from "@/components/app/AppButton";
import { AppPageHeader } from "@/components/app/AppPageHeader";
import { invitationQueryOptions, useInvitation } from "@/domains/membership";
import {
  InvitationResponseCard,
  useAcceptInvitation,
} from "@/features/accept-invitation";
import { isResourceNotFoundError } from "@/lib/resource-not-found";

export const Route = createFileRoute("/polities/invitations/$invitationId")({
  component: InvitationRoute,
  loader: async ({ context, params }) => {
    try {
      await context.queryClient.ensureQueryData(
        invitationQueryOptions({
          invitationId: params.invitationId,
          locale: context.getLocale(),
        }),
      );
    } catch (error) {
      if (isResourceNotFoundError(error)) {
        throw notFound();
      }

      throw error;
    }
  },
});

function InvitationRoute() {
  const { i18n } = useLingui();
  const { invitationId } = Route.useParams();
  const { data: invitation } = useInvitation({
    invitationId,
    locale: i18n.locale,
  });
  const acceptInvitation = useAcceptInvitation();
  const accepted = acceptInvitation.isSuccess;

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <AppBackLink to="/polities">
        <Trans>Back to Polities</Trans>
      </AppBackLink>

      <AppPageHeader
        description={
          accepted ? (
            <Trans>Your invitation response has been recorded.</Trans>
          ) : (
            <Trans>
              Review what joining this polity means before you respond.
            </Trans>
          )
        }
        eyebrow={
          accepted ? (
            <Trans>Membership Updated</Trans>
          ) : (
            <Trans>Pending Invitation</Trans>
          )
        }
        title={invitation.polityName}
      />

      <InvitationResponseCard
        accepted={accepted}
        invitation={invitation}
        isAccepting={acceptInvitation.isPending}
        onAccept={() => acceptInvitation.mutate({ invitationId })}
        renderPolitiesLink={(label, variant = "default") => (
          <AppLinkButton to="/polities" variant={variant}>
            {label}
          </AppLinkButton>
        )}
      />
    </div>
  );
}
