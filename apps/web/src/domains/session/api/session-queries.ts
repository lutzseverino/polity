import {
  type QueryClient,
  queryOptions,
  useSuspenseQuery,
} from "@tanstack/react-query";

import {
  readCurrentSession,
  renewCurrentSession,
} from "@/domains/session/api/session-requests";
import type { Session } from "@/domains/session/lib/session";
import {
  isRecoverableSessionEnd,
  isSessionUnavailableError,
} from "@/domains/session/lib/session-lifecycle";

type CurrentSessionQuery = Readonly<{
  locale: string;
}>;

export const currentSessionQueryKey = ["session", "current"] as const;

/**
 * The cached current session. Its query function only reads, so a background refetch cannot renew
 * Cardo's idle deadline. Restoration is the explicit operation that may refresh.
 */
export function currentSessionQueryOptions({ locale }: CurrentSessionQuery) {
  return queryOptions({
    queryFn: ({ signal }) =>
      readCurrentSession({ acceptedLanguage: locale, signal }),
    queryKey: currentSessionQueryKey,
    retry: false,
    staleTime: 30_000,
  });
}

/**
 * Restores the current session for an intentional entry into a protected path: a fresh cached
 * session is reused, otherwise one read runs, and only an expired authorization credential
 * escalates to a single coordinated refresh. Every stated lifecycle reason ends the session here
 * because Identity has already closed it and no refresh can recover it.
 *
 * This fetches rather than ensures, because `ensureQueryData` returns any cached value regardless of
 * age. A session cached past `staleTime` would otherwise be accepted while its authorization
 * credential has already expired, sending a still-refreshable session to sign-in on the first
 * protected read.
 */
export async function ensureRestoredSession(
  queryClient: QueryClient,
  { locale }: CurrentSessionQuery,
) {
  try {
    return await queryClient.fetchQuery(currentSessionQueryOptions({ locale }));
  } catch (error) {
    if (!isSessionUnavailableError(error)) throw error;
    if (!isRecoverableSessionEnd(error.reason)) throw error;

    const session = await renewCurrentSession({ acceptedLanguage: locale });
    setCurrentSession(queryClient, session);
    return session;
  }
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
