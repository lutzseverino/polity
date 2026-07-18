import { type QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createRouter, type RouterHistory } from "@tanstack/react-router";

import { setTerminalUnauthorizedHandler } from "@/api/http-client";
import { i18n } from "@/app/i18n/i18n";
import { createAppQueryClient } from "@/app/query/create-query-client";
import {
  clearCurrentSession,
  clearSessionDependentQueries,
} from "@/domains/session";
import { readAppLocalDestination } from "@/lib/app-local-destination";
import { routeTree } from "@/routeTree.gen";

export type AppRouterContext = Readonly<{
  getLocale: () => string;
  queryClient: QueryClient;
}>;

export function createAppRouter(
  history?: RouterHistory,
  queryClient = createAppQueryClient(),
) {
  const appRouter = createRouter({
    context: {
      getLocale: () => i18n.locale,
      queryClient,
    },
    defaultPendingMs: 150,
    defaultPreload: "intent",
    defaultPreloadStaleTime: 0,
    history,
    routeTree,
    scrollRestoration: true,
    Wrap: ({ children }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    ),
  });
  let terminalTransitionPending = false;

  setTerminalUnauthorizedHandler(() => {
    if (
      terminalTransitionPending ||
      appRouter.state.location.pathname === "/sign-in"
    ) {
      return;
    }
    terminalTransitionPending = true;
    const { hash, pathname, searchStr } = appRouter.state.location;
    const returnTo = readAppLocalDestination(
      `${pathname}${searchStr}${hash ? `#${hash}` : ""}`,
    );
    clearCurrentSession(queryClient);
    void appRouter
      .navigate({
        search: returnTo ? { returnTo } : {},
        to: "/sign-in",
      })
      .then(() => {
        clearSessionDependentQueries(queryClient);
      })
      .finally(() => {
        terminalTransitionPending = false;
      });
  });

  return appRouter;
}

export const router = createAppRouter();

declare module "@tanstack/react-router" {
  interface Register {
    router: ReturnType<typeof createAppRouter>;
  }
}
