import { Trans, useLingui } from "@lingui/react/macro";
import type { ReactNode } from "react";

import { AppBadge } from "@/components/app/AppBadge";
import {
  AppCard,
  AppCardAction,
  AppCardContent,
  AppCardDescription,
  AppCardHeader,
  AppCardTitle,
} from "@/components/app/AppCard";
import { AppText } from "@/components/app/AppText";
import type { Motion } from "@/domains/motion/lib/motion";

type MotionSummaryProps = Readonly<{
  action?: ReactNode;
  motion: Motion;
}>;

export function MotionSummary({ action, motion }: MotionSummaryProps) {
  const { t } = useLingui();
  const statusLabel =
    motion.status === "voting"
      ? t`Voting open`
      : motion.status === "enacted"
        ? t`Enacted`
        : t`Rejected`;

  return (
    <AppCard
      className="h-full transition-[background-color,box-shadow] group-hover/link-surface:bg-muted/40 group-hover/link-surface:ring-foreground/20"
      size="sm"
    >
      <AppCardHeader>
        <div className="mb-1 flex flex-wrap items-center gap-2">
          <AppBadge
            variant={motion.status === "voting" ? "default" : "secondary"}
          >
            {statusLabel}
          </AppBadge>
          <AppText as="span" variant="caption">
            {motion.category}
          </AppText>
        </div>
        <AppCardTitle>{motion.title}</AppCardTitle>
        <AppCardDescription>
          {motion.status === "voting" ? (
            <Trans>Closes {motion.closesAtLabel.toLowerCase()}</Trans>
          ) : (
            <Trans>Voting {motion.closesAtLabel.toLowerCase()}</Trans>
          )}
        </AppCardDescription>
        {action ? <AppCardAction>{action}</AppCardAction> : null}
      </AppCardHeader>
      <AppCardContent>
        <AppText className="line-clamp-2" variant="supporting">
          {motion.body}
        </AppText>
      </AppCardContent>
    </AppCard>
  );
}
