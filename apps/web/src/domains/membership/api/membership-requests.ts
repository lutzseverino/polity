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
  page: Readonly<{
    number: number;
    totalPages: number;
  }>;
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

async function getMembershipInvitationPage(
  page: number,
  { acceptedLanguage, signal }: LocalizedRequestOptions,
): Promise<MembershipInvitationPageResponse> {
  return httpClient.request<MembershipInvitationPageResponse>({
    acceptedLanguage,
    method: "GET",
    params: { page, size: 100 },
    signal,
    url: "/invitations",
  });
}

export async function listMembershipInvitations({
  acceptedLanguage,
  signal,
}: LocalizedRequestOptions): Promise<readonly MembershipInvitation[]> {
  const invitations: MembershipInvitation[] = [];
  let pageNumber = 0;
  let totalPages = 1;

  while (pageNumber < totalPages) {
    const response = await getMembershipInvitationPage(pageNumber, {
      acceptedLanguage,
      signal,
    });
    invitations.push(
      ...response.content.map((invitation) =>
        toMembershipInvitation(invitation, acceptedLanguage),
      ),
    );
    totalPages = response.page.totalPages;
    pageNumber += 1;
  }

  return invitations;
}

export async function getMembershipInvitation(
  invitationId: string,
  options: LocalizedRequestOptions,
) {
  let pageNumber = 0;
  let totalPages = 1;

  while (pageNumber < totalPages) {
    const response = await getMembershipInvitationPage(pageNumber, options);
    const invitation = response.content.find(
      (candidate) => candidate.id === invitationId,
    );
    if (invitation) {
      return toMembershipInvitation(invitation, options.acceptedLanguage);
    }
    totalPages = response.page.totalPages;
    pageNumber += 1;
  }

  throw new ResourceNotFoundError("Membership invitation", invitationId);
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
