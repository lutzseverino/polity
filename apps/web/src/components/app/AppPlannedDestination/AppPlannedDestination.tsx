import { Trans } from "@lingui/react/macro";
import type { ReactNode } from "react";

import {
  AppCard,
  AppCardContent,
  AppCardDescription,
  AppCardHeader,
  AppCardTitle,
} from "@/components/app/AppCard";
import { AppText } from "@/components/app/AppText";

type AppPlannedDestinationProps = Readonly<{
  className?: string;
  description: ReactNode;
  label?: ReactNode;
  title: ReactNode;
  titleId?: string;
}>;

export function AppPlannedDestination({
  className,
  description,
  label,
  title,
  titleId,
}: AppPlannedDestinationProps) {
  return (
    <section aria-labelledby={titleId} className={className}>
      <AppCard>
        <AppCardHeader>
          <AppCardTitle id={titleId}>{title}</AppCardTitle>
          <AppCardDescription>
            {label ?? <Trans>Planned destination</Trans>}
          </AppCardDescription>
        </AppCardHeader>
        <AppCardContent>
          <AppText className="max-w-prose" variant="supporting">
            {description}
          </AppText>
        </AppCardContent>
      </AppCard>
    </section>
  );
}
