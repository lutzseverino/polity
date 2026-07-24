import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import {
  getMembershipInvitationCompletion,
  type MembershipInvitationCompletionStatus,
  requestMembershipInvitationCompletion,
} from "@/domains/membership";

const completionPollIntervalMs = 1_500;

export function shouldPollMembershipInvitationCompletion(
  status: MembershipInvitationCompletionStatus | undefined,
) {
  return status === "requested" || status === "awaiting_identity";
}

function completionQueryKey(token: string) {
  return ["memberships", "invitation-tokens", token, "completion"] as const;
}

export function useMembershipInvitationCompletion(
  token: string,
  acceptedLanguage: string,
) {
  const queryClient = useQueryClient();
  const [polling, setPolling] = useState(false);
  const request = useMutation({
    mutationFn: () =>
      requestMembershipInvitationCompletion(token, { acceptedLanguage }),
    mutationKey: completionQueryKey(token),
    onSuccess: (completion) => {
      queryClient.setQueryData(completionQueryKey(token), completion);
      setPolling(true);
    },
  });
  const completion = useQuery({
    enabled: polling,
    queryFn: ({ signal }) =>
      getMembershipInvitationCompletion(token, { acceptedLanguage, signal }),
    queryKey: completionQueryKey(token),
    refetchInterval: (query) =>
      shouldPollMembershipInvitationCompletion(query.state.data?.status)
        ? completionPollIntervalMs
        : false,
    retry: false,
  });

  return {
    completion: completion.data,
    error: request.error ?? completion.error,
    isPending: request.isPending,
    request: request.mutate,
  };
}
