import { listInboxItemFixtures } from "@/domains/inbox/lib/inbox-fixtures";
import { projectMembershipInvitationToInboxTask } from "@/domains/inbox/lib/inbox-projectors";
import { listMembershipInvitations } from "@/domains/membership";

type RequestOptions = Readonly<{
  signal?: AbortSignal;
}>;

export async function listInboxItems({ signal }: RequestOptions = {}) {
  signal?.throwIfAborted();

  const invitations = await listMembershipInvitations({ signal });

  return [
    ...listInboxItemFixtures(),
    ...invitations.map(projectMembershipInvitationToInboxTask),
  ];
}
