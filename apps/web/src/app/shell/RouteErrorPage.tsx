import { Trans } from "@lingui/react/macro";
import type { ErrorComponentProps } from "@tanstack/react-router";
import { AlertTriangle } from "lucide-react";

import {
  AppAlert,
  AppAlertDescription,
  AppAlertTitle,
} from "@/components/app/AppAlert";
import { AppLinkButton } from "@/components/app/AppLinkButton";

export function RouteErrorPage(_props: ErrorComponentProps) {
  return (
    <main className="flex min-h-svh items-center justify-center bg-muted/30 p-4">
      <div className="w-full max-w-md space-y-4">
        <AppAlert>
          <AlertTriangle aria-hidden="true" />
          <AppAlertTitle>
            <Trans>We couldn’t open that page</Trans>
          </AppAlertTitle>
          <AppAlertDescription>
            <Trans>Something went wrong while opening this page.</Trans>
          </AppAlertDescription>
        </AppAlert>
        <AppLinkButton to="/polities">
          <Trans>View my polities</Trans>
        </AppLinkButton>
      </div>
    </main>
  );
}
