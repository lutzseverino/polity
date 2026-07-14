import { AppSkeleton } from "@/components/app/AppSkeleton";

export function RouteLoadingPage() {
  const { t } = useLingui();

  return (
    <main
      aria-label={t`Loading page`}
      className="mx-auto w-full max-w-5xl space-y-4 p-4 sm:p-6 md:p-8"
      role="status"
    >
      <span className="sr-only">
        <Trans>Loading page</Trans>
      </span>
      <AppSkeleton className="h-8 w-48" />
      <AppSkeleton className="h-24 w-full" />
      <div className="grid gap-4 md:grid-cols-2">
        <AppSkeleton className="h-40 w-full" />
        <AppSkeleton className="h-40 w-full" />
      </div>
    </main>
  );
}

import { Trans, useLingui } from "@lingui/react/macro";
