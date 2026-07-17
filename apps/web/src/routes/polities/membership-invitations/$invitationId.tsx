import { msg } from "@lingui/core/macro";
import { useLingui } from "@lingui/react/macro";
import { createFileRoute, notFound } from "@tanstack/react-router";
import { AppCard } from "@/components/app/AppCard";
import { AppLinkButton } from "@/components/app/AppLinkButton";
import { AppPageLayout } from "@/components/app/AppPageLayout";
import {
  membershipInvitationQueryOptions,
  useMembershipInvitation,
} from "@/domains/membership";
import { AcceptMembershipInvitationWorkflow } from "@/features/accept-membership-invitation";
import { isResourceNotFoundError } from "@/lib/resource-not-found";

export const Route = createFileRoute(
  "/polities/membership-invitations/$invitationId",
)({
  component: MembershipInvitationRoute,
  loader: async ({ context, params }) => {
    try {
      const invitation = await context.queryClient.ensureQueryData(
        membershipInvitationQueryOptions({
          invitationId: params.invitationId,
          locale: context.getLocale(),
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
      back: { label: msg`Back to Polities`, target: { to: "/polities" } },
      compactLabel: msg`Invitation`,
      compactNavigation: "hidden",
      level: "task",
      section: "polities",
      showPrimaryAction: false,
    },
  },
});

function MembershipInvitationRoute() {
  const { i18n } = useLingui();
  const { invitationId } = Route.useParams();
  const navigate = Route.useNavigate();
  const { data: invitation } = useMembershipInvitation({
    invitationId,
    locale: i18n.locale,
  });

  return (
    <AppPageLayout measure="narrow">
      <AppCard className="gap-0 py-0">
        <AcceptMembershipInvitationWorkflow
          headingLevel="h1"
          invitation={invitation}
          locale={i18n.locale}
          onDismiss={() => {
            void navigate({ to: "/polities" });
          }}
          renderPolitiesLink={(label) => (
            <AppLinkButton
              className="min-h-11 w-full sm:min-h-9 sm:w-auto"
              size="lg"
              to="/polities"
            >
              {label}
            </AppLinkButton>
          )}
        />
      </AppCard>
    </AppPageLayout>
  );
}
