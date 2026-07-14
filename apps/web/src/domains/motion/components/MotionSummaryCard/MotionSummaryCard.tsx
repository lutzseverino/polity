import { Trans, useLingui } from "@lingui/react/macro";
import { Link } from "@tanstack/react-router";
import { ArrowRight } from "lucide-react";

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

type MotionSummaryCardProps = Readonly<{
  motion: Motion;
  polityId: string;
}>;

export function MotionSummaryCard({
  motion,
  polityId,
}: MotionSummaryCardProps) {
  const { t } = useLingui();
  const statusLabel =
    motion.status === "voting"
      ? t`Voting open`
      : motion.status === "enacted"
        ? t`Enacted`
        : t`Rejected`;

  return (
    <AppCard size="sm">
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
        <AppCardAction>
          <Link
            aria-label={t`Open ${motion.title}`}
            className="inline-flex size-8 items-center justify-center rounded-lg text-muted-foreground hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            params={{ motionId: motion.id, polityId }}
            to="/polities/$polityId/motions/$motionId"
          >
            <ArrowRight aria-hidden="true" className="size-4" />
          </Link>
        </AppCardAction>
      </AppCardHeader>
      <AppCardContent>
        <AppText className="line-clamp-2" variant="supporting">
          {motion.body}
        </AppText>
      </AppCardContent>
    </AppCard>
  );
}
