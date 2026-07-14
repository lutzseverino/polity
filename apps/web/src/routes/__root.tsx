import { createRootRouteWithContext } from "@tanstack/react-router";

import type { AppRouterContext } from "@/app/router";
import { AppShell } from "@/app/shell/AppShell";
import { RouteErrorPage } from "@/app/shell/RouteErrorPage";
import { RouteLoadingPage } from "@/app/shell/RouteLoadingPage";
import { RouteNotFoundPage } from "@/app/shell/RouteNotFoundPage";
import { inboxItemsQueryOptions } from "@/domains/inbox";
import { politiesQueryOptions } from "@/domains/polity";

export const Route = createRootRouteWithContext<AppRouterContext>()({
  component: AppShell,
  errorComponent: RouteErrorPage,
  loader: ({ context }) => {
    const locale = context.getLocale();

    return Promise.all([
      context.queryClient.ensureQueryData(inboxItemsQueryOptions({ locale })),
      context.queryClient.ensureQueryData(politiesQueryOptions({ locale })),
    ]);
  },
  notFoundComponent: RouteNotFoundPage,
  pendingComponent: RouteLoadingPage,
});
