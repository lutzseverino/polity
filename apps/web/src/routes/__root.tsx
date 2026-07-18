import {
  createRootRouteWithContext,
  Outlet,
  redirect,
} from "@tanstack/react-router";

import { hasHttpResponseStatus } from "@/api/http-client";
import type { AppRouterContext } from "@/app/router";
import { AppShell } from "@/app/shell/AppShell";
import { RouteErrorPage } from "@/app/shell/RouteErrorPage";
import { RouteLoadingPage } from "@/app/shell/RouteLoadingPage";
import { RouteNotFoundPage } from "@/app/shell/RouteNotFoundPage";
import { inboxItemsQueryOptions } from "@/domains/inbox";
import { membershipInvitationQueryOptions } from "@/domains/membership";
import { polityOptionsQueryOptions } from "@/domains/polity";
import {
  currentSessionQueryKey,
  currentSessionQueryOptions,
  isSessionUnavailableError,
  type Session,
} from "@/domains/session";
import {
  type AcceptMembershipInvitationTask,
  readAcceptMembershipInvitationTask,
} from "@/features/accept-membership-invitation";
import { readAppLocalDestination } from "@/lib/app-local-destination";

type AppSearch = Readonly<{
  returnTo?: string;
  task?: AcceptMembershipInvitationTask;
}>;

function isPublicPath(pathname: string) {
  return (
    pathname === "/sign-in" ||
    /^\/polities\/invitations\/[^/]+\/?$/.test(pathname)
  );
}

function PublicRouteFrame() {
  return (
    <div className="min-h-svh bg-muted/30">
      <main className="mx-auto w-full max-w-7xl px-4 py-5 sm:px-6 md:px-8 md:py-8">
        <Outlet />
      </main>
    </div>
  );
}

function RootRoute() {
  const { isPublic } = Route.useRouteContext();
  return isPublic ? <PublicRouteFrame /> : <AppShell />;
}

export const Route = createRootRouteWithContext<AppRouterContext>()({
  validateSearch: (search): AppSearch => ({
    returnTo: readAppLocalDestination(search.returnTo),
    task: readAcceptMembershipInvitationTask(search.task),
  }),
  beforeLoad: async ({ context, location }) => {
    if (isPublicPath(location.pathname)) {
      return {
        isAuthenticated: Boolean(
          context.queryClient.getQueryData<Session>(currentSessionQueryKey),
        ),
        isPublic: true,
      };
    }

    try {
      await context.queryClient.ensureQueryData(
        currentSessionQueryOptions({ locale: context.getLocale() }),
      );
      return { isAuthenticated: true, isPublic: false };
    } catch (error) {
      if (!isSessionUnavailableError(error)) throw error;
      throw redirect({
        search: {
          returnTo: readAppLocalDestination(location.href),
        },
        to: "/sign-in",
      });
    }
  },
  component: RootRoute,
  errorComponent: RouteErrorPage,
  loaderDeps: ({ search }) => ({ task: search.task }),
  loader: async ({ context, deps, location }) => {
    if (context.isPublic) return;
    const locale = context.getLocale();

    try {
      return await Promise.all([
        context.queryClient.ensureQueryData(inboxItemsQueryOptions({ locale })),
        context.queryClient.ensureQueryData(
          polityOptionsQueryOptions({ locale }),
        ),
        deps.task
          ? context.queryClient.ensureQueryData(
              membershipInvitationQueryOptions({
                invitationId: deps.task.invitationId,
                locale,
              }),
            )
          : Promise.resolve(),
      ]);
    } catch (error) {
      if (!hasHttpResponseStatus(error, 401)) throw error;
      throw redirect({
        search: { returnTo: readAppLocalDestination(location.href) },
        to: "/sign-in",
      });
    }
  },
  notFoundComponent: RouteNotFoundPage,
  pendingComponent: RouteLoadingPage,
});
