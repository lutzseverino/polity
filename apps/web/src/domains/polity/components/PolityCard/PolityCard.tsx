/* eslint-disable react-refresh/only-export-components -- compound component parts share one public API */
import type { ComponentProps, ReactNode } from "react";

import {
  AppCard,
  AppCardContent,
  AppCardDescription,
  AppCardHeader,
  AppCardTitle,
} from "@/components/app/AppCard";
import { cn } from "@/lib/utils";

function PolityCardRoot({
  className,
  ...props
}: ComponentProps<typeof AppCard>) {
  return (
    <AppCard
      className={cn(
        "h-full transition-colors group-hover:bg-muted/40",
        className,
      )}
      {...props}
    />
  );
}

function PolityCardHeader({
  badges,
  children,
  className,
  ...props
}: ComponentProps<typeof AppCardHeader> &
  Readonly<{
    badges?: ReactNode;
  }>) {
  return (
    <AppCardHeader className={className} {...props}>
      {badges ? (
        <div className="mb-2 flex flex-wrap items-center gap-2">{badges}</div>
      ) : null}
      {children}
    </AppCardHeader>
  );
}

function PolityCardIdentity({
  action,
  children,
  className,
  icon,
  ...props
}: ComponentProps<"div"> &
  Readonly<{
    action?: ReactNode;
    icon?: ReactNode;
  }>) {
  return (
    <div className={cn("flex min-w-0 items-start gap-3", className)} {...props}>
      {icon ? (
        <span className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg bg-muted">
          {icon}
        </span>
      ) : null}
      <div className="min-w-0 flex-1">{children}</div>
      {action}
    </div>
  );
}

function PolityCardTitle(props: ComponentProps<typeof AppCardTitle>) {
  return <AppCardTitle {...props} />;
}

function PolityCardDescription(
  props: ComponentProps<typeof AppCardDescription>,
) {
  return <AppCardDescription {...props} />;
}

function PolityCardContent({
  className,
  ...props
}: ComponentProps<typeof AppCardContent>) {
  return <AppCardContent className={cn("space-y-4", className)} {...props} />;
}

function PolityCardMeta({ className, ...props }: ComponentProps<"div">) {
  return (
    <div
      className={cn("flex flex-wrap gap-x-5 gap-y-2", className)}
      {...props}
    />
  );
}

export const PolityCard = Object.assign(PolityCardRoot, {
  Content: PolityCardContent,
  Description: PolityCardDescription,
  Header: PolityCardHeader,
  Identity: PolityCardIdentity,
  Meta: PolityCardMeta,
  Title: PolityCardTitle,
});
