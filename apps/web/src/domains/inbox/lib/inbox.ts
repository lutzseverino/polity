export type InboxCategory = "needs-action" | "updates";

type InboxItemPresentation = Readonly<{
  category: InboxCategory;
  description: string;
  id: string;
  isUnread: boolean;
  polityName?: string;
  timeLabel: string;
  title: string;
}>;

export type InboxTaskItem = InboxItemPresentation &
  Readonly<{
    category: "needs-action";
    source:
      | Readonly<{
          invitationId: string;
          kind: "membership-invitation";
        }>
      | Readonly<{
          kind: "motion-vote";
          motionId: string;
          polityId: string;
        }>
      | Readonly<{
          kind: "candidacy-response";
          motionId: string;
          polityId: string;
        }>;
  }>;

export type InboxUpdateItem = InboxItemPresentation &
  Readonly<{
    category: "updates";
    source: Readonly<{
      kind: "motion-result";
      motionId: string;
      polityId: string;
    }>;
  }>;

export type InboxItem = InboxTaskItem | InboxUpdateItem;
