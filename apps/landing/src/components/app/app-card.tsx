import type { HTMLAttributes, ReactNode } from "react";

import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

type AppCardElement = "article" | "div";
type AppCardPadding = "none" | "md" | "lg";
type AppCardTone = "default" | "primary";

type AppCardProps = Readonly<
  HTMLAttributes<HTMLElement> & {
    as?: AppCardElement;
    padding?: AppCardPadding;
    tone?: AppCardTone;
  }
>;

type AppCardClusterProps = Readonly<
  HTMLAttributes<HTMLDivElement> & {
    children: ReactNode;
  }
>;

const paddingClassName = {
  lg: "p-7 md:p-9",
  md: "p-6",
  none: "",
} satisfies Record<AppCardPadding, string>;

const toneClassName = {
  default: "bg-card text-card-foreground",
  primary: "bg-primary text-primary-foreground",
} satisfies Record<AppCardTone, string>;

export function AppCard({
  as: Element = "div",
  children,
  className,
  padding = "md",
  tone = "default",
  ...props
}: AppCardProps) {
  const cardClassName = cn(
    "gap-0 rounded-none border-0 py-0 ring-0",
    paddingClassName[padding],
    toneClassName[tone],
    className,
  );

  if (Element === "article") {
    return (
      <article className={cardClassName} {...props}>
        {children}
      </article>
    );
  }

  return (
    <Card className={cardClassName} {...props}>
      {children}
    </Card>
  );
}

export function AppCardCluster({
  className,
  children,
  ...props
}: AppCardClusterProps) {
  return (
    <div className={cn("grid gap-px border bg-border", className)} {...props}>
      {children}
    </div>
  );
}
