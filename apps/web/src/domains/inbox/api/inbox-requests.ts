import { listInboxItemFixtures } from "@/domains/inbox/lib/inbox-fixtures";
import { projectMembershipInvitationToInboxTask } from "@/domains/inbox/lib/inbox-projectors";
import { listMembershipInvitations } from "@/domains/membership";

type RequestOptions = Readonly<{
  acceptedLanguage: string;
  signal?: AbortSignal;
}>;

export async function listInboxItems({
  acceptedLanguage,
  signal,
}: RequestOptions) {
  signal?.throwIfAborted();

  const invitations = await listMembershipInvitations({
    acceptedLanguage,
    signal,
  });

  return [
    ...listInboxItemFixtures(),
    ...invitations.map(projectMembershipInvitationToInboxTask),
  ];
}
