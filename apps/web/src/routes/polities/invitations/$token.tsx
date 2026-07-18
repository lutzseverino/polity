import { msg } from "@lingui/core/macro";
import { useLingui } from "@lingui/react/macro";
import { createFileRoute, notFound } from "@tanstack/react-router";
import { AppCard } from "@/components/app/AppCard";
import { AppLinkButton } from "@/components/app/AppLinkButton";
import { AppPageLayout } from "@/components/app/AppPageLayout";
import {
  membershipInvitationTokenQueryOptions,
  useMembershipInvitationToken,
} from "@/domains/membership";
import { OnboardMembershipInvitationWorkflow } from "@/features/onboard-membership-invitation";
import { isResourceNotFoundError } from "@/lib/resource-not-found";

export const Route = createFileRoute("/polities/invitations/$token")({
  component: MembershipInvitationTokenRoute,
  loader: async ({ context, params }) => {
    try {
      const invitation = await context.queryClient.ensureQueryData(
        membershipInvitationTokenQueryOptions({
          locale: context.getLocale(),
          token: params.token,
        }),
      );
      return { shellLabel: invitation.polityName };
    } catch (error) {
      if (isResourceNotFoundError(error)) {
        throw notFound();
      }
      throw error;
    }
  },
  staticData: {
    shell: {
      compactLabel: msg`Invitation`,
      compactNavigation: "hidden",
      level: "task",
      section: "polities",
      showPrimaryAction: false,
    },
  },
});

function MembershipInvitationTokenRoute() {
  const { i18n } = useLingui();
  const { token } = Route.useParams();
  const { data: invitation } = useMembershipInvitationToken({
    locale: i18n.locale,
    token,
  });

  return (
    <AppPageLayout measure="narrow">
      <AppCard className="gap-0 py-0">
        <OnboardMembershipInvitationWorkflow
          invitation={invitation}
          renderSignInLink={(label) => (
            <AppLinkButton
              search={{ returnTo: "/inbox" }}
              size="lg"
              to="/sign-in"
            >
              {label}
            </AppLinkButton>
          )}
          token={token}
        />
      </AppCard>
    </AppPageLayout>
  );
}
