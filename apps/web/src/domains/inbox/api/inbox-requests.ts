import { listInboxItemFixtures } from "@/domains/inbox/lib/inbox-fixtures";
import { projectPendingInvitationToInboxTask } from "@/domains/inbox/lib/inbox-projectors";
import { listInvitations } from "@/domains/membership";

type RequestOptions = Readonly<{
  signal?: AbortSignal;
}>;

export async function listInboxItems({ signal }: RequestOptions = {}) {
  signal?.throwIfAborted();

  const invitations = await listInvitations({ signal });

  return [
    ...listInboxItemFixtures(),
    ...invitations.map(projectPendingInvitationToInboxTask),
  ];
}
