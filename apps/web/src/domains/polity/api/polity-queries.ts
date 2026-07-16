import {
  queryOptions,
  useQuery,
  useSuspenseQuery,
} from "@tanstack/react-query";

import {
  getPolity,
  getPolityActions,
  getPolityMotion,
  listPolities,
  maximumPolityPageSize,
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
  motion: ({ locale, motionId, polityId }: PolityMotionQuery) =>
    ["polities", "detail", polityId, "motions", motionId, { locale }] as const,
};

export function polityActionsQueryOptions(input: PolityQuery) {
  return queryOptions({
    queryFn: ({ signal }) => getPolityActions(input.polityId, { signal }),
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
    queryFn: ({ signal }) =>
      listPolities({
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
    queryFn: ({ signal }) => getPolity(input.polityId, { signal }),
    queryKey: polityQueryKeys.detail(input),
  });
}

export function polityMotionQueryOptions(input: PolityMotionQuery) {
  return queryOptions({
    queryFn: ({ signal }) =>
      getPolityMotion(input.polityId, input.motionId, { signal }),
    queryKey: polityQueryKeys.motion(input),
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

function selectPolityOptions(
  polities: Awaited<ReturnType<typeof listPolities>>,
) {
  return polities.content.map(({ id, name }) => ({ id, name }));
}

export function polityOptionsQueryOptions(input: LocalizedQuery) {
  return {
    ...politiesQueryOptions({ ...input, size: maximumPolityPageSize }),
    select: selectPolityOptions,
  };
}

export function usePolityOptions(input: LocalizedQuery) {
  return useSuspenseQuery(polityOptionsQueryOptions(input));
}
