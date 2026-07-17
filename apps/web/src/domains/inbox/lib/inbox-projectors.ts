import type { InboxTaskItem } from "@/domains/inbox/lib/inbox";
import type { MembershipInvitation } from "@/domains/membership";

export function projectMembershipInvitationToInboxTask(
  invitation: MembershipInvitation,
): InboxTaskItem {
  return {
    category: "needs-action",
    description: `${invitation.invitedByName} invited you to join this polity.`,
    id: `membership-invitation:${invitation.id}`,
    isUnread: true,
    polityName: invitation.polityName,
    source: {
      invitationId: invitation.id,
      kind: "membership-invitation",
    },
    timeLabel: invitation.invitedAtLabel,
    title: `Invitation to join ${invitation.polityName}`,
  };
}
