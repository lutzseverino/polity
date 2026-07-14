import { Plural, Trans, useLingui } from "@lingui/react/macro";
import { Bell } from "lucide-react";

import { AppBadge } from "@/components/app/AppBadge";
import { AppButton, AppLinkButton } from "@/components/app/AppButton";
import {
  AppPopover,
  AppPopoverContent,
  AppPopoverTrigger,
} from "@/components/app/AppPopover";
import { AppText } from "@/components/app/AppText";
import {
  InboxItemLink,
  type RenderInvitationLink,
} from "@/domains/inbox/components/InboxItemLink";
import type { InboxItem } from "@/domains/inbox/lib/inbox";
import {
  countOpenInboxTasks,
  selectInboxPreviewItems,
} from "@/domains/inbox/lib/inbox-selectors";

type InboxPreviewProps = Readonly<{
  items: readonly InboxItem[];
  renderInvitationLink: RenderInvitationLink;
}>;

export function InboxPreview({
  items,
  renderInvitationLink,
}: InboxPreviewProps) {
  const { t } = useLingui();
  const openTaskCount = countOpenInboxTasks(items);
  const previewItems = selectInboxPreviewItems(items);

  return (
    <AppPopover>
      <AppPopoverTrigger
        aria-label={t`Open Inbox, ${openTaskCount} items need action`}
        className="relative"
        render={<AppButton size="icon-lg" variant="ghost" />}
      >
        <Bell aria-hidden="true" />
        <span className="sr-only">
          <Trans>Open Inbox</Trans>
        </span>
        {openTaskCount > 0 ? (
          <span
            aria-hidden="true"
            className="absolute -top-1 -right-1 flex min-w-4 items-center justify-center rounded-full bg-primary px-1 text-[0.625rem] leading-4 font-semibold text-primary-foreground"
          >
            {openTaskCount}
          </span>
        ) : null}
      </AppPopoverTrigger>
      <AppPopoverContent
        align="end"
        className="w-[min(26rem,calc(100vw-2rem))] overscroll-contain p-3"
      >
        <div className="mb-3 flex items-center justify-between gap-3">
          <div>
            <AppText variant="subsectionTitle">
              <Trans>Inbox</Trans>
            </AppText>
            <AppText className="mt-0.5" variant="caption">
              <Plural
                value={openTaskCount}
                one="# item needs action"
                other="# items need action"
              />
            </AppText>
          </div>
          <AppBadge>{openTaskCount}</AppBadge>
        </div>
        <div className="space-y-2">
          {previewItems.map((item) => (
            <InboxItemLink
              compact
              item={item}
              key={item.id}
              renderInvitationLink={renderInvitationLink}
            />
          ))}
        </div>
        <AppLinkButton className="mt-3 w-full" to="/inbox" variant="outline">
          <Trans>View All Inbox Items</Trans>
        </AppLinkButton>
      </AppPopoverContent>
    </AppPopover>
  );
}
