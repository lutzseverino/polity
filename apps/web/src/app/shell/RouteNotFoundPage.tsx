import { Trans } from "@lingui/react/macro";
import { SearchX } from "lucide-react";

import {
  AppAlert,
  AppAlertDescription,
  AppAlertTitle,
} from "@/components/app/AppAlert";
import { AppLinkButton } from "@/components/app/AppButton";

export function RouteNotFoundPage() {
  return (
    <main className="flex min-h-svh items-center justify-center bg-muted/30 p-4">
      <div className="w-full max-w-md space-y-4">
        <AppAlert>
          <SearchX aria-hidden="true" />
          <AppAlertTitle>
            <Trans>Page not found</Trans>
          </AppAlertTitle>
          <AppAlertDescription>
            <Trans>The destination you requested does not exist.</Trans>
          </AppAlertDescription>
        </AppAlert>
        <AppLinkButton to="/polities">
          <Trans>View my polities</Trans>
        </AppLinkButton>
      </div>
    </main>
  );
}
