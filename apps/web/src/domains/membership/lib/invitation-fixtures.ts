import type { MembershipInvitation } from "@/domains/membership/lib/membership";

const membershipInvitations: readonly MembershipInvitation[] = [
  {
    id: "invitation-supper-club",
    invitedAtLabel: "Today",
    invitedByName: "Sam Ortega",
    polityName: "Sunday Supper Club",
  },
  {
    id: "invitation-garden-cooperative",
    invitedAtLabel: "Yesterday",
    invitedByName: "Mira Chen",
    polityName: "Garden Cooperative",
  },
  {
    id: "invitation-book-circle",
    invitedAtLabel: "3 days ago",
    invitedByName: "Alex Rivera",
    polityName: "Local Book Circle",
  },
  {
    id: "invitation-cabin-council",
    invitedAtLabel: "5 days ago",
    invitedByName: "Jon Bell",
    polityName: "Cabin Council",
  },
];

export function findMembershipInvitationFixture(invitationId: string) {
  return membershipInvitations.find(
    (invitation) => invitation.id === invitationId,
  );
}

export function listMembershipInvitationFixtures() {
  return membershipInvitations;
}
