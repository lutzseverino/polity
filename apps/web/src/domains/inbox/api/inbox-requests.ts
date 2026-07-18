import {
  projectMembershipInvitationToInboxTask,
  projectMotionToInboxItem,
} from "@/domains/inbox/lib/inbox-projectors";
import { listMembershipInvitations } from "@/domains/membership";
import { listAllPolities, listPolityMotionResponses } from "@/domains/polity";

type RequestOptions = Readonly<{
  acceptedLanguage: string;
  signal?: AbortSignal;
}>;

export async function listInboxItems({
  acceptedLanguage,
  signal,
}: RequestOptions) {
  signal?.throwIfAborted();

  const [invitations, polities] = await Promise.all([
    listMembershipInvitations({ acceptedLanguage, signal }),
    listAllPolities({ acceptedLanguage, signal }),
  ]);
  const motionPages = await Promise.all(
    polities.map(async (polity) => ({
      motions: await listPolityMotionResponses(polity.id, {
        acceptedLanguage,
        signal,
      }),
      polity,
    })),
  );

  return [
    ...motionPages.flatMap(({ motions, polity }) =>
      motions.flatMap((motion) => {
        const item = projectMotionToInboxItem(motion, polity, acceptedLanguage);
        return item ? [item] : [];
      }),
    ),
    ...invitations.map(projectMembershipInvitationToInboxTask),
  ];
}
