import {
  findMembershipInvitationFixture,
  listMembershipInvitationFixtures,
} from "@/domains/membership/lib/invitation-fixtures";
import type {
  MembershipInvitationCompletion,
  MembershipInvitationTokenContext,
} from "@/domains/membership/lib/membership";
import { ResourceNotFoundError } from "@/lib/resource-not-found";

type RequestOptions = Readonly<{
  signal?: AbortSignal;
}>;

export function listMembershipInvitations({
  signal,
}: RequestOptions = {}): Promise<
  ReturnType<typeof listMembershipInvitationFixtures>
> {
  signal?.throwIfAborted();

  return Promise.resolve(listMembershipInvitationFixtures());
}

export function getMembershipInvitation(
  invitationId: string,
  { signal }: RequestOptions = {},
) {
  signal?.throwIfAborted();

  const invitation = findMembershipInvitationFixture(invitationId);

  return invitation
    ? Promise.resolve(invitation)
    : Promise.reject(
        new ResourceNotFoundError("Membership invitation", invitationId),
      );
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
