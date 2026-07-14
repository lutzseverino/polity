import type { InboxTaskItem } from "@/domains/inbox/lib/inbox";
import type { PendingInvitation } from "@/domains/membership";

export function projectPendingInvitationToInboxTask(
  invitation: PendingInvitation,
): InboxTaskItem {
  return {
    category: "needs-action",
    description: `${invitation.invitedBy} invited you to join this polity.`,
    id: `membership-invitation:${invitation.id}`,
    isUnread: true,
    polityName: invitation.polityName,
    source: {
      invitationId: invitation.id,
      kind: "membership-invitation",
    },
    timeLabel: invitation.receivedLabel,
    title: `Invitation to join ${invitation.polityName}`,
  };
}
