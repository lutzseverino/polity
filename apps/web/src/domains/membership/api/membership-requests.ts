import { createHttpClient } from "@/api/http-client";
import type {
  MembershipInvitation,
  MembershipInvitationCompletion,
  MembershipInvitationTokenContext,
} from "@/domains/membership/lib/membership";
import { ResourceNotFoundError } from "@/lib/resource-not-found";

type RequestOptions = Readonly<{
  signal?: AbortSignal;
}>;

type LocalizedRequestOptions = RequestOptions &
  Readonly<{
    acceptedLanguage: string;
  }>;

type MembershipInvitationResponse = Readonly<{
  id: string;
  invitedAt: string;
  invitedByName: string;
  polityName: string;
}>;

type MembershipInvitationPageResponse = Readonly<{
  content: readonly MembershipInvitationResponse[];
}>;

const httpClient = createHttpClient();

function toMembershipInvitation(
  invitation: MembershipInvitationResponse,
  acceptedLanguage: string,
): MembershipInvitation {
  return {
    id: invitation.id,
    invitedAtLabel: new Intl.DateTimeFormat(acceptedLanguage, {
      dateStyle: "medium",
    }).format(new Date(invitation.invitedAt)),
    invitedByName: invitation.invitedByName,
    polityName: invitation.polityName,
  };
}

export async function listMembershipInvitations({
  acceptedLanguage,
  signal,
}: LocalizedRequestOptions): Promise<readonly MembershipInvitation[]> {
  const response = await httpClient.request<MembershipInvitationPageResponse>({
    acceptedLanguage,
    method: "GET",
    params: { size: 100 },
    signal,
    url: "/invitations",
  });

  return response.content.map((invitation) =>
    toMembershipInvitation(invitation, acceptedLanguage),
  );
}

export async function getMembershipInvitation(
  invitationId: string,
  options: LocalizedRequestOptions,
) {
  const invitation = (await listMembershipInvitations(options)).find(
    (candidate) => candidate.id === invitationId,
  );

  if (!invitation) {
    throw new ResourceNotFoundError("Membership invitation", invitationId);
  }
  return invitation;
}

async function invitationTokenResponse<T>(
  response: Response,
  token: string,
): Promise<T> {
  if (response.status === 404) {
    throw new ResourceNotFoundError("Membership invitation token", token);
  }
  if (response.status === 410) {
    throw new Error("This membership invitation is no longer available.");
  }
  if (!response.ok) {
    throw new Error(
      `Membership invitation request failed (${response.status}).`,
    );
  }
  return (await response.json()) as T;
}

function invitationTokenPath(token: string, suffix = "") {
  return `/api/v1/invitation-tokens/${encodeURIComponent(token)}${suffix}`;
}

export async function getMembershipInvitationByToken(
  token: string,
  { signal }: RequestOptions = {},
) {
  const response = await fetch(invitationTokenPath(token), {
    cache: "no-store",
    headers: { Accept: "application/json" },
    signal,
  });
  return invitationTokenResponse<MembershipInvitationTokenContext>(
    response,
    token,
  );
}

export async function requestMembershipInvitationCompletion(token: string) {
  const response = await fetch(invitationTokenPath(token, "/completion"), {
    cache: "no-store",
    headers: { Accept: "application/json" },
    method: "POST",
  });
  return invitationTokenResponse<MembershipInvitationCompletion>(
    response,
    token,
  );
}

export async function getMembershipInvitationCompletion(
  token: string,
  { signal }: RequestOptions = {},
) {
  const response = await fetch(invitationTokenPath(token, "/completion"), {
    cache: "no-store",
    headers: { Accept: "application/json" },
    signal,
  });
  return invitationTokenResponse<MembershipInvitationCompletion>(
    response,
    token,
  );
}
