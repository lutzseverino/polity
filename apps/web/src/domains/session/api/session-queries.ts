import {
  type QueryClient,
  queryOptions,
  useSuspenseQuery,
} from "@tanstack/react-query";

import { restoreCurrentSession } from "@/domains/session/api/session-requests";
import type { Session } from "@/domains/session/lib/session";

type CurrentSessionQuery = Readonly<{
  locale: string;
}>;

export const currentSessionQueryKey = ["session", "current"] as const;

export function currentSessionQueryOptions({ locale }: CurrentSessionQuery) {
  return queryOptions({
    queryFn: ({ signal }) =>
      restoreCurrentSession({ acceptedLanguage: locale, signal }),
    queryKey: currentSessionQueryKey,
    retry: false,
    staleTime: 30_000,
  });
}

export function useCurrentSession(input: CurrentSessionQuery) {
  return useSuspenseQuery(currentSessionQueryOptions(input));
}

export function setCurrentSession(queryClient: QueryClient, session: Session) {
  queryClient.setQueryData(currentSessionQueryKey, session);
}

export function clearCurrentSession(queryClient: QueryClient) {
  queryClient.removeQueries({ queryKey: currentSessionQueryKey });
}

export function clearSessionDependentQueries(queryClient: QueryClient) {
  queryClient.removeQueries({
    predicate: (query) => query.meta?.requiresSession === true,
  });
}
