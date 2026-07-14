import { queryOptions, useSuspenseQuery } from "@tanstack/react-query";

import {
  getPolity,
  getPolityMotion,
  listPolities,
} from "@/domains/polity/api/polity-requests";

type LocalizedQuery = Readonly<{
  locale: string;
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
  list: ({ locale }: LocalizedQuery) =>
    ["polities", "list", { locale }] as const,
  motion: ({ locale, motionId, polityId }: PolityMotionQuery) =>
    ["polities", "detail", polityId, "motions", motionId, { locale }] as const,
};

export function politiesQueryOptions(input: LocalizedQuery) {
  return queryOptions({
    queryFn: ({ signal }) => listPolities({ signal }),
    queryKey: polityQueryKeys.list(input),
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

export function usePolities(input: LocalizedQuery) {
  return useSuspenseQuery(politiesQueryOptions(input));
}

export function usePolity(input: PolityQuery) {
  return useSuspenseQuery(polityQueryOptions(input));
}

export function usePolityMotion(input: PolityMotionQuery) {
  return useSuspenseQuery(polityMotionQueryOptions(input));
}

function selectPolityOptions(
  polities: Awaited<ReturnType<typeof listPolities>>,
) {
  return polities.map(({ id, name }) => ({ id, name }));
}

export function usePolityOptions(input: LocalizedQuery) {
  return useSuspenseQuery({
    ...politiesQueryOptions(input),
    select: selectPolityOptions,
  });
}
