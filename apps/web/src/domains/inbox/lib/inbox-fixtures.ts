import type { InboxItem } from "@/domains/inbox/lib/inbox";

const inboxItems: readonly InboxItem[] = [
  {
    category: "needs-action",
    description: "Voting closes today at 19:00.",
    id: "vote-shared-dinner",
    isUnread: true,
    polityName: "The Thursday Assembly",
    source: {
      kind: "motion-vote",
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
    source: {
      kind: "candidacy-response",
      motionId: "tribune-election",
      polityId: "thursday-assembly",
    },
    timeLabel: "Yesterday",
    title: "Respond to Your Nomination",
  },
  {
    category: "updates",
    description:
      "The motion was adopted with 5 votes in support and 2 against.",
    id: "autumn-cabin-result",
    isUnread: false,
    polityName: "The Thursday Assembly",
    source: {
      kind: "motion-result",
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
