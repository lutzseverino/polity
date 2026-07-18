import { msg } from "@lingui/core/macro";
import { Trans, useLingui } from "@lingui/react/macro";
import { createFileRoute } from "@tanstack/react-router";
import { AppPageHeader } from "@/components/app/AppPageHeader";
import { AppPageLayout } from "@/components/app/AppPageLayout";
import { AppPlannedDestination } from "@/components/app/AppPlannedDestination";
import { AppText } from "@/components/app/AppText";
import { useCurrentSession } from "@/domains/session";
import { SignOutWorkflow } from "@/features/sign-out";

export const Route = createFileRoute("/me/")({
  component: MeRoute,
  staticData: {
    shell: {
      label: msg`Me`,
      level: "root",
      section: "me",
      target: { to: "/me" },
    },
  },
});

function MeRoute() {
  const { i18n } = useLingui();
  const navigate = Route.useNavigate();
  const { data: session } = useCurrentSession({ locale: i18n.locale });

  return (
    <AppPageLayout measure="focused">
      <AppPageHeader title={<Trans>Me</Trans>} />
      <AppPlannedDestination
        description={
          <Trans>
            Profile, account, language, accessibility, and appearance
            preferences will live here.
          </Trans>
        }
        title={<Trans>Account and Preferences</Trans>}
        titleId="planned-page-title"
      />
      <section aria-labelledby="session-heading" className="space-y-3">
        <AppText as="h2" id="session-heading" variant="sectionTitle">
          <Trans>Session</Trans>
        </AppText>
        <AppText variant="supporting">
          {session.principal.name ?? session.principal.email}
        </AppText>
        <SignOutWorkflow
          onSignedOut={() => void navigate({ to: "/sign-in" })}
        />
      </section>
    </AppPageLayout>
  );
}
