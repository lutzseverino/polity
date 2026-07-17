import { queryOptions, useSuspenseQuery } from "@tanstack/react-query";

import { listInboxItems } from "@/domains/inbox/api/inbox-requests";

type InboxQuery = Readonly<{
  locale: string;
}>;

const inboxQueryKeys = {
  all: ["inbox"] as const,
  list: ({ locale }: InboxQuery) => ["inbox", "list", { locale }] as const,
};

export function inboxItemsQueryOptions(input: InboxQuery) {
  return queryOptions({
    queryFn: ({ signal }) =>
      listInboxItems({ acceptedLanguage: input.locale, signal }),
    queryKey: inboxQueryKeys.list(input),
  });
}

export function useInboxItems(input: InboxQuery) {
  return useSuspenseQuery(inboxItemsQueryOptions(input));
}
