import { type QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createRouter, type RouterHistory } from "@tanstack/react-router";

import { i18n } from "@/app/i18n/i18n";
import { createAppQueryClient } from "@/app/query/create-query-client";
import { routeTree } from "@/routeTree.gen";

export type AppRouterContext = Readonly<{
  getLocale: () => string;
  queryClient: QueryClient;
}>;

export function createAppRouter(
  history?: RouterHistory,
  queryClient = createAppQueryClient(),
) {
  return createRouter({
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
}

export const router = createAppRouter();

declare module "@tanstack/react-router" {
  interface Register {
    router: ReturnType<typeof createAppRouter>;
  }
}
