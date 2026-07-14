import type { InboxItem } from "@/domains/inbox/lib/inbox";

const inboxItems: readonly InboxItem[] = [
  {
    category: "needs-action",
    description: "Voting closes today at 19:00.",
    id: "vote-shared-dinner",
    isUnread: true,
    polityName: "The Thursday Assembly",
    target: {
      kind: "motion",
      motionId: "shared-dinner",
      polityId: "thursday-assembly",
    },
    timeLabel: "2 hours ago",
    title: "Vote on Shared Thursday Dinner",
  },
  {
    category: "needs-action",
    description: "Confirm whether you accept your nomination for Tribune.",
    id: "tribune-candidacy",
    isUnread: true,
    polityName: "The Thursday Assembly",
    target: {
      kind: "motion",
      motionId: "tribune-election",
      polityId: "thursday-assembly",
    },
    timeLabel: "Yesterday",
    title: "Respond to Your Nomination",
  },
  {
    category: "needs-action",
    description: "Sam Ortega invited you to become a citizen.",
    id: "invitation-supper-club",
    isUnread: true,
    polityName: "Sunday Supper Club",
    target: {
      invitationId: "invitation-supper-club",
      kind: "invitation",
    },
    timeLabel: "Today",
    title: "Invitation to Sunday Supper Club",
  },
  {
    category: "needs-action",
    description: "Mira Chen invited you to become a citizen.",
    id: "invitation-garden-cooperative",
    isUnread: true,
    polityName: "Garden Cooperative",
    target: {
      invitationId: "invitation-garden-cooperative",
      kind: "invitation",
    },
    timeLabel: "Yesterday",
    title: "Invitation to Garden Cooperative",
  },
  {
    category: "needs-action",
    description: "Alex Rivera invited you to become a citizen.",
    id: "invitation-book-circle",
    isUnread: true,
    polityName: "Local Book Circle",
    target: {
      invitationId: "invitation-book-circle",
      kind: "invitation",
    },
    timeLabel: "3 days ago",
    title: "Invitation to Local Book Circle",
  },
  {
    category: "needs-action",
    description: "Jon Bell invited you to become a citizen.",
    id: "invitation-cabin-council",
    isUnread: true,
    polityName: "Cabin Council",
    target: {
      invitationId: "invitation-cabin-council",
      kind: "invitation",
    },
    timeLabel: "5 days ago",
    title: "Invitation to Cabin Council",
  },
  {
    category: "updates",
    description:
      "The motion was adopted with 5 votes in support and 2 against.",
    id: "autumn-cabin-result",
    isUnread: false,
    polityName: "The Thursday Assembly",
    target: {
      kind: "motion",
      motionId: "autumn-cabin-budget",
      polityId: "thursday-assembly",
    },
    timeLabel: "4 days ago",
    title: "Autumn Cabin Budget Was Adopted",
  },
];

export function listInboxItemFixtures(): readonly InboxItem[] {
  return inboxItems;
}
