import {
  queryOptions,
  useQuery,
  useSuspenseQuery,
} from "@tanstack/react-query";

import {
  getPolity,
  getPolityActions,
  getPolityGovernment,
  getPolityMotion,
  getPolityOfficialRecord,
  listAllPolities,
  listPolities,
  normalizePolityPage,
  normalizePolityPageSize,
  normalizePolityQuery,
} from "@/domains/polity/api/polity-requests";

type LocalizedQuery = Readonly<{
  locale: string;
}>;

type PolityListQuery = LocalizedQuery &
  Readonly<{
    page?: number;
    query?: string;
    size?: number;
  }>;

type NormalizedPolityListQuery = LocalizedQuery &
  Readonly<{
    page: number;
    query?: string;
    size: number;
  }>;

type PolityQuery = LocalizedQuery &
  Readonly<{
    polityId: string;
  }>;

type PolityMotionQuery = PolityQuery &
  Readonly<{
    motionId: string;
  }>;

const polityQueryKeys = {
  all: ["polities"] as const,
  detail: ({ locale, polityId }: PolityQuery) =>
    ["polities", "detail", polityId, { locale }] as const,
  actions: ({ locale, polityId }: PolityQuery) =>
    ["polities", "detail", polityId, "actions", { locale }] as const,
  list: ({ locale, page, query, size }: NormalizedPolityListQuery) =>
    ["polities", "list", { locale, page, query, size }] as const,
  government: ({ locale, polityId }: PolityQuery) =>
    ["polities", "detail", polityId, "government", { locale }] as const,
  motion: ({ locale, motionId, polityId }: PolityMotionQuery) =>
    ["polities", "detail", polityId, "motions", motionId, { locale }] as const,
  record: ({ locale, polityId }: PolityQuery) =>
    ["polities", "detail", polityId, "record", { locale }] as const,
};

export function polityActionsQueryOptions(input: PolityQuery) {
  return queryOptions({
    meta: { requiresSession: true },
    queryFn: ({ signal }) =>
      getPolityActions(input.polityId, {
        acceptedLanguage: input.locale,
        signal,
      }),
    queryKey: polityQueryKeys.actions(input),
  });
}

export const polityListQueryKey = ["polities", "list"] as const;

export function politiesQueryOptions(input: PolityListQuery) {
  const normalizedInput = {
    ...input,
    page: normalizePolityPage(input.page),
    query: normalizePolityQuery(input.query),
    size: normalizePolityPageSize(input.size),
  };

  return queryOptions({
    meta: { requiresSession: true },
    queryFn: ({ signal }) =>
      listPolities({
        acceptedLanguage: input.locale,
        page: normalizedInput.page,
        query: normalizedInput.query,
        signal,
        size: normalizedInput.size,
      }),
    queryKey: polityQueryKeys.list(normalizedInput),
  });
}

export function polityQueryOptions(input: PolityQuery) {
  return queryOptions({
    meta: { requiresSession: true },
    queryFn: ({ signal }) =>
      getPolity(input.polityId, {
        acceptedLanguage: input.locale,
        signal,
      }),
    queryKey: polityQueryKeys.detail(input),
  });
}

export function polityMotionQueryOptions(input: PolityMotionQuery) {
  return queryOptions({
    meta: { requiresSession: true },
    queryFn: ({ signal }) =>
      getPolityMotion(input.polityId, input.motionId, {
        acceptedLanguage: input.locale,
        signal,
      }),
    queryKey: polityQueryKeys.motion(input),
  });
}

export function polityGovernmentQueryOptions(input: PolityQuery) {
  return queryOptions({
    meta: { requiresSession: true },
    queryFn: ({ signal }) =>
      getPolityGovernment(input.polityId, {
        acceptedLanguage: input.locale,
        signal,
      }),
    queryKey: polityQueryKeys.government(input),
  });
}

export function polityRecordQueryOptions(input: PolityQuery) {
  return queryOptions({
    meta: { requiresSession: true },
    queryFn: ({ signal }) =>
      getPolityOfficialRecord(input.polityId, {
        acceptedLanguage: input.locale,
        signal,
      }),
    queryKey: polityQueryKeys.record(input),
  });
}

export function usePolities(input: PolityListQuery) {
  return useSuspenseQuery(politiesQueryOptions(input));
}

export function usePolity(input: PolityQuery) {
  return useSuspenseQuery(polityQueryOptions(input));
}

export function usePolityActions(input: PolityQuery) {
  return useQuery(polityActionsQueryOptions(input));
}

export function usePolityMotion(input: PolityMotionQuery) {
  return useSuspenseQuery(polityMotionQueryOptions(input));
}

export function usePolityGovernment(input: PolityQuery) {
  return useSuspenseQuery(polityGovernmentQueryOptions(input));
}

export function usePolityRecord(input: PolityQuery) {
  return useSuspenseQuery(polityRecordQueryOptions(input));
}

export function polityOptionsQueryOptions(input: LocalizedQuery) {
  return queryOptions({
    meta: { requiresSession: true },
    queryFn: ({ signal }) =>
      listAllPolities({ acceptedLanguage: input.locale, signal }),
    queryKey: ["polities", "options", { locale: input.locale }] as const,
    select: (polities) => polities.map(({ id, name }) => ({ id, name })),
  });
}

export function usePolityOptions(input: LocalizedQuery) {
  return useSuspenseQuery(polityOptionsQueryOptions(input));
}
