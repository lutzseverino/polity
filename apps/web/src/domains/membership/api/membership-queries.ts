import { queryOptions, useSuspenseQuery } from "@tanstack/react-query";

import {
  getInvitation,
  listInvitations,
} from "@/domains/membership/api/membership-requests";

type LocalizedQuery = Readonly<{
  locale: string;
}>;

type InvitationQuery = LocalizedQuery &
  Readonly<{
    invitationId: string;
  }>;

const membershipQueryKeys = {
  all: ["memberships"] as const,
  invitation: ({ invitationId, locale }: InvitationQuery) =>
    ["memberships", "invitations", "detail", invitationId, { locale }] as const,
  invitations: ({ locale }: LocalizedQuery) =>
    ["memberships", "invitations", "list", { locale }] as const,
};

export function invitationsQueryOptions(input: LocalizedQuery) {
  return queryOptions({
    queryFn: ({ signal }) => listInvitations({ signal }),
    queryKey: membershipQueryKeys.invitations(input),
  });
}

export function invitationQueryOptions(input: InvitationQuery) {
  return queryOptions({
    queryFn: ({ signal }) => getInvitation(input.invitationId, { signal }),
    queryKey: membershipQueryKeys.invitation(input),
  });
}

export function useInvitations(input: LocalizedQuery) {
  return useSuspenseQuery(invitationsQueryOptions(input));
}

export function useInvitation(input: InvitationQuery) {
  return useSuspenseQuery(invitationQueryOptions(input));
}
