import { listInboxItemFixtures } from "@/domains/inbox/lib/inbox-fixtures";

type RequestOptions = Readonly<{
  signal?: AbortSignal;
}>;

export function listInboxItems({
  signal,
}: RequestOptions = {}): Promise<ReturnType<typeof listInboxItemFixtures>> {
  signal?.throwIfAborted();

  return Promise.resolve(listInboxItemFixtures());
}
