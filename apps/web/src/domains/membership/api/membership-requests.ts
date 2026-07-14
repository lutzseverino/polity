import {
  findInvitationFixture,
  listInvitationFixtures,
} from "@/domains/membership/lib/invitation-fixtures";
import { ResourceNotFoundError } from "@/lib/resource-not-found";

type RequestOptions = Readonly<{
  signal?: AbortSignal;
}>;

export function listInvitations({
  signal,
}: RequestOptions = {}): Promise<ReturnType<typeof listInvitationFixtures>> {
  signal?.throwIfAborted();

  return Promise.resolve(listInvitationFixtures());
}

export function getInvitation(
  invitationId: string,
  { signal }: RequestOptions = {},
) {
  signal?.throwIfAborted();

  const invitation = findInvitationFixture(invitationId);

  return invitation
    ? Promise.resolve(invitation)
    : Promise.reject(new ResourceNotFoundError("Invitation", invitationId));
}
