import { createRootRouteWithContext } from "@tanstack/react-router";

import type { AppRouterContext } from "@/app/router";
import { AppShell } from "@/app/shell/AppShell";
import { RouteErrorPage } from "@/app/shell/RouteErrorPage";
import { RouteLoadingPage } from "@/app/shell/RouteLoadingPage";
import { RouteNotFoundPage } from "@/app/shell/RouteNotFoundPage";
import { inboxItemsQueryOptions } from "@/domains/inbox";
import { invitationQueryOptions } from "@/domains/membership";
import { polityOptionsQueryOptions } from "@/domains/polity";
import {
  type InvitationTask,
  readInvitationTask,
} from "@/features/accept-invitation";

type AppSearch = Readonly<{
  task?: InvitationTask;
}>;

export const Route = createRootRouteWithContext<AppRouterContext>()({
  component: AppShell,
  errorComponent: RouteErrorPage,
  validateSearch: (search): AppSearch => ({
    task: readInvitationTask(search.task),
  }),
  loaderDeps: ({ search }) => ({ task: search.task }),
  loader: ({ context, deps }) => {
    const locale = context.getLocale();

    return Promise.all([
      context.queryClient.ensureQueryData(inboxItemsQueryOptions({ locale })),
      context.queryClient.ensureQueryData(
        polityOptionsQueryOptions({ locale }),
      ),
      deps.task
        ? context.queryClient.ensureQueryData(
            invitationQueryOptions({
              invitationId: deps.task.invitationId,
              locale,
            }),
          )
        : Promise.resolve(),
    ]);
  },
  notFoundComponent: RouteNotFoundPage,
  pendingComponent: RouteLoadingPage,
});
