import type { InboxItem, InboxTaskItem } from "@/domains/inbox/lib/inbox";
import type { MembershipInvitation } from "@/domains/membership";
import type { MotionResponse } from "@/domains/polity";

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

export function projectMotionToInboxItem(
  motion: MotionResponse,
  polity: Readonly<{ id: string; name: string }>,
  locale: string,
): InboxItem | undefined {
  const timeLabel = new Intl.DateTimeFormat(locale, {
    dateStyle: "medium",
  }).format(new Date(motion.openedAt));

  if (motion.status === "voting" && motion.actions.castVote.available) {
    return {
      category: "needs-action",
      description: `Voting closes ${new Intl.DateTimeFormat(locale, { dateStyle: "medium", timeStyle: "short" }).format(new Date(motion.votingClosesAt))}.`,
      id: `motion-vote:${motion.id}`,
      isUnread: true,
      polityName: polity.name,
      source: { kind: "motion-vote", motionId: motion.id, polityId: polity.id },
      timeLabel,
      title: `Vote on ${motion.title}`,
    };
  }

  if (motion.status === "voting" && motion.actions.respondCandidacy.available) {
    return {
      category: "needs-action",
      description: `Confirm whether you accept your nomination${motion.officeElection ? ` for ${motion.officeElection.officeName}` : ""}.`,
      id: `candidacy-response:${motion.id}`,
      isUnread: true,
      polityName: polity.name,
      source: {
        kind: "candidacy-response",
        motionId: motion.id,
        polityId: polity.id,
      },
      timeLabel,
      title: "Respond to Your Nomination",
    };
  }

  if (motion.status !== "voting" && motion.certification) {
    return {
      category: "updates",
      description: motion.certification.passed
        ? "The motion was adopted."
        : "The motion was rejected.",
      id: `motion-result:${motion.id}`,
      isUnread: false,
      polityName: polity.name,
      source: {
        kind: "motion-result",
        motionId: motion.id,
        polityId: polity.id,
      },
      timeLabel,
      title: `${motion.title} Was ${motion.certification.passed ? "Adopted" : "Rejected"}`,
    };
  }

  return undefined;
}
