import { queryOptions, useSuspenseQuery } from "@tanstack/react-query";

import {
  getMembershipInvitation,
  getMembershipInvitationByToken,
  listMembershipInvitations,
} from "@/domains/membership/api/membership-requests";

type LocalizedQuery = Readonly<{
  locale: string;
}>;

type MembershipInvitationQuery = LocalizedQuery &
  Readonly<{
    invitationId: string;
  }>;

type MembershipInvitationTokenQuery = LocalizedQuery &
  Readonly<{
    token: string;
  }>;

const membershipQueryKeys = {
  all: ["memberships"] as const,
  membershipInvitation: ({ invitationId, locale }: MembershipInvitationQuery) =>
    ["memberships", "invitations", "detail", invitationId, { locale }] as const,
  membershipInvitations: ({ locale }: LocalizedQuery) =>
    ["memberships", "invitations", "list", { locale }] as const,
  membershipInvitationToken: ({
    locale,
    token,
  }: MembershipInvitationTokenQuery) =>
    ["memberships", "invitation-tokens", token, { locale }] as const,
};

export function membershipInvitationsQueryOptions(input: LocalizedQuery) {
  return queryOptions({
    queryFn: ({ signal }) => listMembershipInvitations({ signal }),
    queryKey: membershipQueryKeys.membershipInvitations(input),
  });
}

export function membershipInvitationQueryOptions(
  input: MembershipInvitationQuery,
) {
  return queryOptions({
    queryFn: ({ signal }) =>
      getMembershipInvitation(input.invitationId, { signal }),
    queryKey: membershipQueryKeys.membershipInvitation(input),
  });
}

export function useMembershipInvitation(input: MembershipInvitationQuery) {
  return useSuspenseQuery(membershipInvitationQueryOptions(input));
}

export function membershipInvitationTokenQueryOptions(
  input: MembershipInvitationTokenQuery,
) {
  return queryOptions({
    queryFn: ({ signal }) =>
      getMembershipInvitationByToken(input.token, { signal }),
    queryKey: membershipQueryKeys.membershipInvitationToken(input),
  });
}

export function useMembershipInvitationToken(
  input: MembershipInvitationTokenQuery,
) {
  return useSuspenseQuery(membershipInvitationTokenQueryOptions(input));
}
