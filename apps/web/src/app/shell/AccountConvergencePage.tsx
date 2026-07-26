import { Trans } from "@lingui/react/macro";
import { useRouter } from "@tanstack/react-router";
import { AlertTriangle, Clock3 } from "lucide-react";
import type { ReactNode } from "react";

import {
  AppAlert,
  AppAlertDescription,
  AppAlertTitle,
} from "@/components/app/AppAlert";
import { AppText } from "@/components/app/AppText";
import { useCurrentAccountState } from "@/domains/account";
import { SignOutWorkflow } from "@/features/sign-out";

function AccountConvergenceFrame({
  children,
}: Readonly<{ children: ReactNode }>) {
  return (
    <main className="flex min-h-svh items-center justify-center bg-muted/30 p-4">
      <div className="w-full max-w-md space-y-4">{children}</div>
    </main>
  );
}

function AccountConvergencePendingPage() {
  return (
    <AccountConvergenceFrame>
      <AppAlert aria-live="polite">
        <Clock3 aria-hidden="true" />
        <AppAlertTitle>
          <AppText as="h1" variant="sectionTitle">
            <Trans>Preparing your Polity account</Trans>
          </AppText>
        </AppAlertTitle>
        <AppAlertDescription>
          <Trans>
            Your account is ready. We’re finishing access to Polity before
            loading your workspace.
          </Trans>
        </AppAlertDescription>
      </AppAlert>
    </AccountConvergenceFrame>
  );
}

export function AccountConvergenceFailedPage() {
  const router = useRouter();

  return (
    <AccountConvergenceFrame>
      <AppAlert aria-live="assertive" variant="destructive">
        <AlertTriangle aria-hidden="true" />
        <AppAlertTitle>
          <AppText as="h1" variant="sectionTitle">
            <Trans>We couldn’t finish your Polity access</Trans>
          </AppText>
        </AppAlertTitle>
        <AppAlertDescription>
          <Trans>
            Your account was created, but its access could not be applied.
            Signing in again will not repair it. Please contact support before
            continuing.
          </Trans>
        </AppAlertDescription>
      </AppAlert>
      <SignOutWorkflow
        onSignedOut={() => {
          void router.navigate({ to: "/sign-in" });
        }}
      />
    </AccountConvergenceFrame>
  );
}

export function RootPendingPage({
  fallback,
}: Readonly<{ fallback: ReactNode }>) {
  const { data: account } = useCurrentAccountState();

  return account?.grants.status === "pending" ? (
    <AccountConvergencePendingPage />
  ) : (
    fallback
  );
}
