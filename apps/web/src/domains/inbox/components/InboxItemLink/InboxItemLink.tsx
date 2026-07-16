import { Trans } from "@lingui/react/macro";
import { Link } from "@tanstack/react-router";
import { ArrowRight } from "lucide-react";
import type { ReactNode } from "react";

import { AppBadge } from "@/components/app/AppBadge";
import { AppText } from "@/components/app/AppText";
import type { InboxItem } from "@/domains/inbox/lib/inbox";
import { cn } from "@/lib/utils";

type InboxItemLinkProps = Readonly<{
  compact?: boolean;
  item: InboxItem;
  renderInvitationLink: RenderInvitationLink;
}>;

export type RenderInvitationLink = (
  props: Readonly<{
    children: ReactNode;
    className: string;
    invitationId: string;
  }>,
) => ReactNode;

function InboxTargetLink({
  children,
  className,
  item,
  renderInvitationLink,
}: Readonly<{
  children: ReactNode;
  className: string;
  item: InboxItem;
  renderInvitationLink: RenderInvitationLink;
}>) {
  if (item.source.kind === "membership-invitation") {
    return renderInvitationLink({
      children,
      className,
      invitationId: item.source.invitationId,
    });
  }

  return (
    <Link
      className={className}
      params={{
        motionId: item.source.motionId,
        polityId: item.source.polityId,
      }}
      to="/polities/$polityId/motions/$motionId"
    >
      {children}
    </Link>
  );
}

export function InboxItemLink({
  compact = false,
  item,
  renderInvitationLink,
}: InboxItemLinkProps) {
  return (
    <InboxTargetLink
      className={cn(
        "focus-indicator group flex min-w-0 gap-3 rounded-lg border transition-colors hover:border-foreground/20 hover:bg-muted",
        compact ? "p-3" : "p-4",
      )}
      item={item}
      renderInvitationLink={renderInvitationLink}
    >
      <span
        aria-hidden="true"
        className={cn(
          "mt-1.5 size-2 shrink-0 rounded-full",
          item.isUnread ? "bg-foreground" : "bg-border",
        )}
      />
      <div className="min-w-0 flex-1">
        <div className="flex min-w-0 flex-wrap items-center gap-2">
          <AppText className="min-w-0 break-words" variant="strong">
            {item.title}
          </AppText>
          {item.category === "needs-action" ? (
            <AppBadge variant="outline">
              <Trans>Needs Action</Trans>
            </AppBadge>
          ) : null}
        </div>
        <AppText className="mt-1 line-clamp-2" variant="caption">
          {item.description}
        </AppText>
        <AppText className="mt-2" variant="caption">
          {item.polityName ? `${item.polityName} · ` : ""}
          {item.timeLabel}
        </AppText>
      </div>
      <ArrowRight
        aria-hidden="true"
        className="mt-1 size-4 shrink-0 text-muted-foreground group-hover:text-foreground"
      />
    </InboxTargetLink>
  );
}
