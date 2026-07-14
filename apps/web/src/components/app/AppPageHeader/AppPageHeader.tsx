import type { ReactNode } from "react";

import { AppText } from "@/components/app/AppText";
import { cn } from "@/lib/utils";

type AppPageHeaderProps = Readonly<{
  className?: string;
  /**
   * Hides the route-owned heading visually on compact layouts when the shell
   * top bar already presents the same context. The heading remains accessible.
   */
  compactVisibility?: "hidden" | "visible";
  /** Optional route-owned guidance. Do not add copy only to fill this slot. */
  description?: ReactNode;
  eyebrow?: ReactNode;
  title: ReactNode;
}>;

export function AppPageHeader({
  className,
  compactVisibility = "visible",
  description,
  eyebrow,
  title,
}: AppPageHeaderProps) {
  return (
    <header
      className={cn(
        compactVisibility === "hidden" && "sr-only md:not-sr-only",
        className,
      )}
    >
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
