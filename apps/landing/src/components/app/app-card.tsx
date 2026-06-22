import type { ComponentProps } from "react";

import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { cn } from "@/lib/utils";

type AppCardProps = Readonly<ComponentProps<typeof Card>>;
type AppCardHeaderProps = Readonly<ComponentProps<typeof CardHeader>>;
type AppCardTitleProps = Readonly<ComponentProps<typeof CardTitle>>;
type AppCardDescriptionProps = Readonly<ComponentProps<typeof CardDescription>>;
type AppCardActionProps = Readonly<ComponentProps<typeof CardAction>>;
type AppCardContentProps = Readonly<ComponentProps<typeof CardContent>>;
type AppCardFooterProps = Readonly<ComponentProps<typeof CardFooter>>;

export function AppCard({ className, ...props }: AppCardProps) {
  return (
    <Card
      className={cn("gap-0 rounded-none border py-0 ring-0", className)}
      {...props}
    />
  );
}

export function AppCardHeader({ className, ...props }: AppCardHeaderProps) {
  return (
    <CardHeader
      className={cn("rounded-none border-b px-4 pt-2.5 pb-2.5!", className)}
      {...props}
    />
  );
}

export function AppCardTitle(props: AppCardTitleProps) {
  return <CardTitle {...props} />;
}

export function AppCardDescription(props: AppCardDescriptionProps) {
  return <CardDescription {...props} />;
}

export function AppCardAction(props: AppCardActionProps) {
  return <CardAction {...props} />;
}

export function AppCardContent({ className, ...props }: AppCardContentProps) {
  return <CardContent className={cn("p-5", className)} {...props} />;
}

export function AppCardFooter(props: AppCardFooterProps) {
  return <CardFooter {...props} />;
}
