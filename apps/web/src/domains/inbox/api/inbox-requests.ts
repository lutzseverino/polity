import {
  projectMembershipInvitationToInboxTask,
  projectMotionToInboxItem,
} from "@/domains/inbox/lib/inbox-projectors";
import { listMembershipInvitations } from "@/domains/membership";
import {
  listPolities,
  listPolityMotionResponses,
  maximumPolityPageSize,
} from "@/domains/polity";

type RequestOptions = Readonly<{
  acceptedLanguage: string;
  signal?: AbortSignal;
}>;

export async function listInboxItems({
  acceptedLanguage,
  signal,
}: RequestOptions) {
  signal?.throwIfAborted();

  const [invitations, polityPage] = await Promise.all([
    listMembershipInvitations({ acceptedLanguage, signal }),
    listPolities({
      acceptedLanguage,
      signal,
      size: maximumPolityPageSize,
    }),
  ]);
  const motionPages = await Promise.all(
    polityPage.content.map(async (polity) => ({
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
