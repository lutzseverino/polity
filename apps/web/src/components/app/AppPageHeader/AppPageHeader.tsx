import type { ReactNode } from "react";

import { AppText } from "@/components/app/AppText";

type AppPageHeaderProps = Readonly<{
  className?: string;
  description: ReactNode;
  eyebrow: ReactNode;
  title: ReactNode;
}>;

export function AppPageHeader({
  className,
  description,
  eyebrow,
  title,
}: AppPageHeaderProps) {
  return (
    <header className={className}>
      <AppText variant="eyebrow">{eyebrow}</AppText>
      <AppText as="h1" className="mt-1" variant="pageTitle">
        {title}
      </AppText>
      <AppText className="mt-2 max-w-2xl" variant="supporting">
        {description}
      </AppText>
    </header>
  );
}
