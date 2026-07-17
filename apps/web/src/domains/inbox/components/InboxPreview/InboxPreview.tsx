import { Plural, Trans, useLingui } from "@lingui/react/macro";
import { Bell } from "lucide-react";
import type { ReactNode } from "react";

import { AppBadge } from "@/components/app/AppBadge";
import { AppButton } from "@/components/app/AppButton";
import {
  AppPopover,
  AppPopoverContent,
  AppPopoverTrigger,
} from "@/components/app/AppPopover";
import { AppText } from "@/components/app/AppText";
import {
  InboxItemSummary,
  type RenderInboxItemLink,
} from "@/domains/inbox/components/InboxItemSummary";
import type { InboxItem } from "@/domains/inbox/lib/inbox";
import {
  countOpenInboxTasks,
  selectInboxPreviewItems,
} from "@/domains/inbox/lib/inbox-selectors";

type InboxPreviewProps = Readonly<{
  items: readonly InboxItem[];
  renderInboxItemLink: RenderInboxItemLink;
  renderInboxLink: (label: ReactNode) => ReactNode;
}>;

export function InboxPreview({
  items,
  renderInboxItemLink,
  renderInboxLink,
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
            <InboxItemSummary
              compact
              item={item}
              key={item.id}
              renderLink={renderInboxItemLink}
            />
          ))}
        </div>
        {renderInboxLink(<Trans>View All Inbox Items</Trans>)}
      </AppPopoverContent>
    </AppPopover>
  );
}
