import type { PendingInvitation } from "@/domains/membership/lib/membership";

const pendingInvitations: readonly PendingInvitation[] = [
  {
    id: "invitation-supper-club",
    invitedBy: "Sam Ortega",
    polityName: "Sunday Supper Club",
    receivedLabel: "Today",
  },
  {
    id: "invitation-garden-cooperative",
    invitedBy: "Mira Chen",
    polityName: "Garden Cooperative",
    receivedLabel: "Yesterday",
  },
  {
    id: "invitation-book-circle",
    invitedBy: "Alex Rivera",
    polityName: "Local Book Circle",
    receivedLabel: "3 days ago",
  },
  {
    id: "invitation-cabin-council",
    invitedBy: "Jon Bell",
    polityName: "Cabin Council",
    receivedLabel: "5 days ago",
  },
];

export function findInvitationFixture(invitationId: string) {
  return pendingInvitations.find(
    (invitation) => invitation.id === invitationId,
  );
}

export function listInvitationFixtures() {
  return pendingInvitations;
}
