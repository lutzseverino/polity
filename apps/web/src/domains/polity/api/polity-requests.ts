import {
  findPolityActionAvailabilityFixture,
  findPolityFixture,
  findPolityMotionFixture,
  listPolityFixtures,
} from "@/domains/polity/lib/polity-fixtures";
import type { PageResult } from "@/lib/pagination";
import { ResourceNotFoundError } from "@/lib/resource-not-found";

const defaultPolityPageSize = 50;
export const maximumPolityPageSize = 100;

type RequestOptions = Readonly<{
  signal?: AbortSignal;
}>;

type ListPolitiesOptions = RequestOptions &
  Readonly<{
    page?: number;
    query?: string;
    size?: number;
  }>;

export function normalizePolityQuery(query?: string) {
  const normalized = query?.trim();

  return normalized ? normalized : undefined;
}

export function normalizePolityPage(page?: number) {
  return Number.isSafeInteger(page) && page !== undefined && page >= 0
    ? page
    : 0;
}

export function normalizePolityPageSize(size?: number) {
  if (!Number.isSafeInteger(size) || size === undefined) {
    return defaultPolityPageSize;
  }

  return Math.min(Math.max(size, 1), maximumPolityPageSize);
}

export function listPolities({
  page,
  query,
  signal,
  size,
}: ListPolitiesOptions = {}): Promise<
  PageResult<ReturnType<typeof listPolityFixtures>[number]>
> {
  signal?.throwIfAborted();

  const normalizedPage = normalizePolityPage(page);
  const normalizedSize = normalizePolityPageSize(size);
  const matchingPolities = listPolityFixtures(normalizePolityQuery(query));
  const totalElements = matchingPolities.length;
  const totalPages = Math.ceil(totalElements / normalizedSize);
  const start = normalizedPage * normalizedSize;

  return Promise.resolve({
    content: matchingPolities.slice(start, start + normalizedSize),
    page: {
      number: normalizedPage,
      size: normalizedSize,
      totalElements,
      totalPages,
    },
  });
}

export function getPolity(polityId: string, { signal }: RequestOptions = {}) {
  signal?.throwIfAborted();

  const polity = findPolityFixture(polityId);

  return polity
    ? Promise.resolve(polity)
    : Promise.reject(new ResourceNotFoundError("Polity", polityId));
}

export function getPolityActions(
  polityId: string,
  { signal }: RequestOptions = {},
) {
  signal?.throwIfAborted();

  const actions = findPolityActionAvailabilityFixture(polityId);

  return actions
    ? Promise.resolve(actions)
    : Promise.reject(new ResourceNotFoundError("Polity actions", polityId));
}

export function getPolityMotion(
  polityId: string,
  motionId: string,
  { signal }: RequestOptions = {},
) {
  signal?.throwIfAborted();

  const motion = findPolityMotionFixture(polityId, motionId);

  return motion
    ? Promise.resolve(motion)
    : Promise.reject(new ResourceNotFoundError("Motion", motionId));
}
