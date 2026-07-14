import { mutationOptions, useMutation } from "@tanstack/react-query";

import {
  type AcceptInvitationInput,
  acceptInvitation,
} from "@/features/accept-invitation/api/accept-invitation-request";

function acceptInvitationMutationOptions() {
  return mutationOptions<AcceptInvitationInput, Error, AcceptInvitationInput>({
    mutationFn: acceptInvitation,
    mutationKey: ["memberships", "invitations", "accept"],
  });
}

export function useAcceptInvitation() {
  return useMutation(acceptInvitationMutationOptions());
}
