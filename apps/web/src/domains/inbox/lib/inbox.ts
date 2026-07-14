export type InboxCategory = "needs-action" | "updates";

export type InboxItem = Readonly<{
  category: InboxCategory;
  description: string;
  id: string;
  isUnread: boolean;
  polityName?: string;
  timeLabel: string;
  target:
    | Readonly<{ invitationId: string; kind: "invitation" }>
    | Readonly<{ kind: "motion"; motionId: string; polityId: string }>;
  title: string;
}>;
