import {
  type QueryClient,
  queryOptions,
  useQueryClient,
} from "@tanstack/react-query";
import { useCallback, useSyncExternalStore } from "react";

import {
  getCurrentAccount,
  getOrProvisionCurrentAccount,
} from "@/domains/account/api/account-requests";
import type { PolityAccount } from "@/domains/account/lib/account";

type CurrentAccountQuery = Readonly<{
  locale: string;
  signal?: AbortSignal;
}>;

const currentAccountQueryKey = ["account", "current"] as const;
const accountConvergencePollingIntervalMs = 1_000;

function currentAccountBootstrapQueryOptions({
  locale,
  signal: navigationSignal,
}: CurrentAccountQuery) {
  return queryOptions({
    meta: { requiresSession: true },
    queryFn: ({ signal }) =>
      getOrProvisionCurrentAccount({
        acceptedLanguage: locale,
        signal: navigationSignal ?? signal,
      }),
    queryKey: currentAccountQueryKey,
    retry: false,
    staleTime: Number.POSITIVE_INFINITY,
  });
}

function currentAccountReadQueryOptions({
  locale,
  signal: navigationSignal,
}: CurrentAccountQuery) {
  return queryOptions({
    meta: { requiresSession: true },
    queryFn: ({ signal }) =>
      getCurrentAccount({
        acceptedLanguage: locale,
        signal: navigationSignal ?? signal,
      }),
    queryKey: currentAccountQueryKey,
    retry: false,
    staleTime: 0,
  });
}

function waitForNextPoll(signal?: AbortSignal) {
  return new Promise<void>((resolve, reject) => {
    const onAbort = () => {
      globalThis.clearTimeout(timeout);
      reject(
        signal?.reason instanceof Error
          ? signal.reason
          : new DOMException(
              "Account convergence was cancelled.",
              "AbortError",
            ),
      );
    };
    const timeout = globalThis.setTimeout(() => {
      signal?.removeEventListener("abort", onAbort);
      resolve();
    }, accountConvergencePollingIntervalMs);

    if (signal?.aborted) {
      onAbort();
    } else {
      signal?.addEventListener("abort", onAbort, { once: true });
    }
  });
}

export async function ensureCurrentAccountConverged(
  queryClient: QueryClient,
  input: CurrentAccountQuery,
) {
  let account = queryClient.getQueryData<PolityAccount>(currentAccountQueryKey);
  account ??= await queryClient.fetchQuery(
    currentAccountBootstrapQueryOptions(input),
  );

  while (account.grants.status === "pending") {
    await waitForNextPoll(input.signal);
    account = await queryClient.fetchQuery(
      currentAccountReadQueryOptions(input),
    );
  }

  return account;
}

export function useCurrentAccountState() {
  const queryClient = useQueryClient();
  const subscribe = useCallback(
    (onStoreChange: () => void) =>
      queryClient.getQueryCache().subscribe(onStoreChange),
    [queryClient],
  );
  const getSnapshot = useCallback(
    () => queryClient.getQueryData<PolityAccount>(currentAccountQueryKey),
    [queryClient],
  );

  return { data: useSyncExternalStore(subscribe, getSnapshot, getSnapshot) };
}
