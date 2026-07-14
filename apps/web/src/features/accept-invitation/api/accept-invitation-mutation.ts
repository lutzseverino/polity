import {
  mutationOptions,
  type QueryClient,
  useMutation,
  useQueryClient,
} from "@tanstack/react-query";

import {
  type InboxItem,
  inboxItemsQueryOptions,
  removeInvitationInboxTask,
} from "@/domains/inbox";
import {
  invitationsQueryOptions,
  type PendingInvitation,
} from "@/domains/membership";
import { politiesQueryOptions } from "@/domains/polity";
import {
  type AcceptInvitationInput,
  acceptInvitation,
} from "@/features/accept-invitation/api/accept-invitation-request";

type AcceptInvitationMutationOptions = Readonly<{
  locale: string;
  queryClient: QueryClient;
}>;

function acceptInvitationMutationOptions({
  locale,
  queryClient,
}: AcceptInvitationMutationOptions) {
  return mutationOptions<AcceptInvitationInput, Error, AcceptInvitationInput>({
    mutationFn: acceptInvitation,
    mutationKey: ["memberships", "invitations", "accept"],
    onSuccess: (_, { invitationId }) => {
      queryClient.setQueryData<readonly InboxItem[]>(
        inboxItemsQueryOptions({ locale }).queryKey,
        (items) =>
          items ? removeInvitationInboxTask(items, invitationId) : items,
      );
      queryClient.setQueryData<readonly PendingInvitation[]>(
        invitationsQueryOptions({ locale }).queryKey,
        (invitations) =>
          invitations?.filter((invitation) => invitation.id !== invitationId),
      );
      void queryClient.invalidateQueries({
        queryKey: politiesQueryOptions({ locale }).queryKey,
      });
    },
  });
}

export function useAcceptInvitation({ locale }: Readonly<{ locale: string }>) {
  const queryClient = useQueryClient();

  return useMutation(acceptInvitationMutationOptions({ locale, queryClient }));
}
