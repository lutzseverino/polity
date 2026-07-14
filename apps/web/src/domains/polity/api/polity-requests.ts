import {
  findPolityFixture,
  findPolityMotionFixture,
  listPolityFixtures,
} from "@/domains/polity/lib/polity-fixtures";
import { ResourceNotFoundError } from "@/lib/resource-not-found";

type RequestOptions = Readonly<{
  signal?: AbortSignal;
}>;

export function listPolities({
  signal,
}: RequestOptions = {}): Promise<ReturnType<typeof listPolityFixtures>> {
  signal?.throwIfAborted();

  return Promise.resolve(listPolityFixtures());
}

export function getPolity(polityId: string, { signal }: RequestOptions = {}) {
  signal?.throwIfAborted();

  const polity = findPolityFixture(polityId);

  return polity
    ? Promise.resolve(polity)
    : Promise.reject(new ResourceNotFoundError("Polity", polityId));
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
