import { AppBadge } from "@/components/app/AppBadge";
import type { Polity } from "@/domains/polity/lib/polity";

type ReadinessBadgeProps = Readonly<Pick<Polity, "readiness">>;

export function ReadinessBadge({ readiness }: ReadinessBadgeProps) {
  return (
    <AppBadge variant={readiness === "ready" ? "secondary" : "outline"}>
      {readiness === "ready" ? <Trans>Ready</Trans> : <Trans>Forming</Trans>}
    </AppBadge>
  );
}

import { Trans } from "@lingui/react/macro";
