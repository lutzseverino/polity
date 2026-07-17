import {
  mutationOptions,
  type QueryClient,
  useMutation,
  useQueryClient,
} from "@tanstack/react-query";

import {
  type InboxItem,
  inboxItemsQueryOptions,
  removeMembershipInvitationInboxTask,
} from "@/domains/inbox";
import {
  type MembershipInvitation,
  membershipInvitationsQueryOptions,
} from "@/domains/membership";
import { polityListQueryKey } from "@/domains/polity";
import {
  type AcceptMembershipInvitationInput,
  acceptMembershipInvitation,
} from "@/features/accept-membership-invitation/api/accept-membership-invitation-request";

type AcceptMembershipInvitationMutationOptions = Readonly<{
  locale: string;
  queryClient: QueryClient;
}>;

function acceptMembershipInvitationMutationOptions({
  locale,
  queryClient,
}: AcceptMembershipInvitationMutationOptions) {
  return mutationOptions<
    AcceptMembershipInvitationInput,
    Error,
    AcceptMembershipInvitationInput
  >({
    mutationFn: acceptMembershipInvitation,
    mutationKey: ["memberships", "invitations", "accept"],
    onSuccess: (_, { invitationId }) => {
      queryClient.setQueryData<readonly InboxItem[]>(
        inboxItemsQueryOptions({ locale }).queryKey,
        (items) =>
          items
            ? removeMembershipInvitationInboxTask(items, invitationId)
            : items,
      );
      queryClient.setQueryData<readonly MembershipInvitation[]>(
        membershipInvitationsQueryOptions({ locale }).queryKey,
        (invitations) =>
          invitations?.filter((invitation) => invitation.id !== invitationId),
      );
      void queryClient.invalidateQueries({
        queryKey: polityListQueryKey,
      });
    },
  });
}

export function useAcceptMembershipInvitation({
  locale,
}: Readonly<{ locale: string }>) {
  const queryClient = useQueryClient();

  return useMutation(
    acceptMembershipInvitationMutationOptions({ locale, queryClient }),
  );
}
