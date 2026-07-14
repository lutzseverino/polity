import type {
  InboxCategory,
  InboxItem,
  InboxTaskItem,
} from "@/domains/inbox/lib/inbox";

export function isOpenInboxTask(item: InboxItem): item is InboxTaskItem {
  return item.category === "needs-action";
}

export function countOpenInboxTasks(items: readonly InboxItem[]) {
  return items.filter(isOpenInboxTask).length;
}

export function filterInboxItemsByCategory(
  items: readonly InboxItem[],
  category: InboxCategory,
) {
  return items.filter((item) => item.category === category);
}

export function selectInboxPreviewItems(
  items: readonly InboxItem[],
  limit = 3,
) {
  const tasks = items.filter(isOpenInboxTask);
  const updates = items.filter((item) => !isOpenInboxTask(item));

  return [...tasks, ...updates].slice(0, limit);
}

export function removeInvitationInboxTask(
  items: readonly InboxItem[],
  invitationId: string,
) {
  return items.filter(
    (item) =>
      item.source.kind !== "membership-invitation" ||
      item.source.invitationId !== invitationId,
  );
}
