import { describe, expect, it } from "vitest";

import type { InboxItem } from "@/domains/inbox/lib/inbox";
import {
  countOpenInboxTasks,
  removeMembershipInvitationInboxTask,
  selectInboxPreviewItems,
} from "@/domains/inbox/lib/inbox-selectors";

const items: readonly InboxItem[] = [
  {
    category: "updates",
    description: "An informational update.",
    id: "motion-result:result-1",
    isUnread: true,
    source: {
      kind: "motion-result",
      motionId: "result-1",
      politySlug: "thursday-assembly",
    },
    timeLabel: "Today",
    title: "Result available",
  },
  {
    category: "needs-action",
    description: "This task has been seen but remains open.",
    id: "motion-vote:vote-1",
    isUnread: false,
    source: {
      kind: "motion-vote",
      motionId: "vote-1",
      politySlug: "thursday-assembly",
    },
    timeLabel: "Today",
    title: "Vote now",
  },
  {
    category: "needs-action",
    description: "An invitation is pending.",
    id: "membership-invitation:invitation-1",
    isUnread: true,
    source: {
      invitationId: "invitation-1",
      kind: "membership-invitation",
    },
    timeLabel: "Today",
    title: "Invitation to join",
  },
];

describe("Inbox attention selectors", () => {
  it("counts open tasks independently from unread state", () => {
    expect(countOpenInboxTasks(items)).toBe(2);
  });

  it("prioritizes open tasks in the preview", () => {
    expect(selectInboxPreviewItems(items, 2).map((item) => item.id)).toEqual([
      "motion-vote:vote-1",
      "membership-invitation:invitation-1",
    ]);
  });

  it("removes only the invitation task resolved by its source domain", () => {
    expect(
      removeMembershipInvitationInboxTask(items, "invitation-1").map(
        (item) => item.id,
      ),
    ).toEqual(["motion-result:result-1", "motion-vote:vote-1"]);
  });
});
