import { Trans } from "@lingui/react/macro";
import { type ErrorComponentProps, useRouter } from "@tanstack/react-router";
import { AlertTriangle } from "lucide-react";
import { useEffect } from "react";

import { hasHttpResponseStatus } from "@/api/http-client";
import { RouteLoadingPage } from "@/app/shell/RouteLoadingPage";
import {
  AppAlert,
  AppAlertDescription,
  AppAlertTitle,
} from "@/components/app/AppAlert";
import { AppLinkButton } from "@/components/app/AppLinkButton";
import { readAppLocalDestination } from "@/lib/app-local-destination";

function TerminalUnauthorizedRedirect() {
  const router = useRouter();

  useEffect(() => {
    const { hash, pathname, searchStr } = router.state.location;
    const returnTo = readAppLocalDestination(
      `${pathname}${searchStr}${hash ? `#${hash}` : ""}`,
    );
    void router.navigate({
      search: returnTo ? { returnTo } : {},
      to: "/sign-in",
    });
  }, [router]);

  return <RouteLoadingPage />;
}

export function RouteErrorPage({ error }: ErrorComponentProps) {
  if (hasHttpResponseStatus(error, 401)) {
    return <TerminalUnauthorizedRedirect />;
  }

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
