import { createHttpClient } from "@/api/http-client";
import { ResourceNotFoundError } from "@/lib/resource-not-found";
import { isUuid } from "@/lib/uuid";

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
  if (!isUuid(invitationId)) {
    throw new ResourceNotFoundError("Membership invitation", invitationId);
  }

  return httpClient.request<AcceptedMembership>({
    acceptedLanguage,
    method: "POST",
    url: `/invitations/${encodeURIComponent(invitationId)}/accept`,
  });
}
