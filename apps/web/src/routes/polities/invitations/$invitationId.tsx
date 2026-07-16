import { msg } from "@lingui/core/macro";
import { useLingui } from "@lingui/react/macro";
import { createFileRoute, notFound } from "@tanstack/react-router";

import { AppLinkButton } from "@/components/app/AppButton";
import { AppCard } from "@/components/app/AppCard";
import { AppPageLayout } from "@/components/app/AppPageLayout";
import { invitationQueryOptions } from "@/domains/membership";
import {
  InvitationResponse,
  invitationResponseDescriptionId,
  invitationResponseTitleId,
} from "@/features/accept-invitation";
import { isResourceNotFoundError } from "@/lib/resource-not-found";

export const Route = createFileRoute("/polities/invitations/$invitationId")({
  component: InvitationRoute,
  loader: async ({ context, params }) => {
    try {
      const invitation = await context.queryClient.ensureQueryData(
        invitationQueryOptions({
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

function InvitationRoute() {
  const { i18n } = useLingui();
  const { invitationId } = Route.useParams();
  const navigate = Route.useNavigate();

  return (
    <AppPageLayout measure="narrow">
      <AppCard className="gap-0 py-0">
        <InvitationResponse
          descriptionId={invitationResponseDescriptionId}
          headingLevel="h1"
          invitationId={invitationId}
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
          titleId={invitationResponseTitleId}
        />
      </AppCard>
    </AppPageLayout>
  );
}
