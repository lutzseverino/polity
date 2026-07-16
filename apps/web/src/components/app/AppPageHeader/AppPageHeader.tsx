import type { ReactNode } from "react";

import { AppText } from "@/components/app/AppText";
import { cn } from "@/lib/utils";

type AppPageHeaderProps = Readonly<{
  className?: string;
  /** Optional route-owned guidance. Do not add copy only to fill this slot. */
  description?: ReactNode;
  eyebrow?: ReactNode;
  title: ReactNode;
}>;

export function AppPageHeader({
  className,
  description,
  eyebrow,
  title,
}: AppPageHeaderProps) {
  return (
    <header className={className} data-slot="page-header">
      {eyebrow ? <AppText variant="eyebrow">{eyebrow}</AppText> : null}
      <AppText as="h1" className={cn(eyebrow && "mt-1")} variant="pageTitle">
        {title}
      </AppText>
      {description ? (
        <AppText className="mt-2 max-w-2xl" variant="supporting">
          {description}
        </AppText>
      ) : null}
    </header>
  );
}
