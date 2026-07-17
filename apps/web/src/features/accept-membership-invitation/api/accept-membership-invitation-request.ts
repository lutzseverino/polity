import { createHttpClient } from "@/api/http-client";

export type AcceptMembershipInvitationInput = Readonly<{
  invitationId: string;
}>;

type AcceptMembershipInvitationRequest = AcceptMembershipInvitationInput &
  Readonly<{
    acceptedLanguage: string;
  }>;

export type AcceptedMembership = Readonly<{
  id: string;
  status: "active";
}>;

const httpClient = createHttpClient();

export function acceptMembershipInvitation({
  acceptedLanguage,
  invitationId,
}: AcceptMembershipInvitationRequest) {
  return httpClient.request<AcceptedMembership>({
    acceptedLanguage,
    method: "POST",
    url: `/invitations/${encodeURIComponent(invitationId)}/accept`,
  });
}
