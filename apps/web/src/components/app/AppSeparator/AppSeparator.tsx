import type { ComponentProps } from "react";

import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";

type AppSeparatorProps = ComponentProps<typeof Separator> &
  Readonly<{
    variant?: "gradient" | "solid";
  }>;

const separatorVariantClassName: Readonly<
  Record<NonNullable<AppSeparatorProps["variant"]>, string>
> = {
  gradient:
    "bg-transparent data-horizontal:bg-linear-to-r data-horizontal:from-transparent data-horizontal:via-border data-horizontal:to-transparent data-vertical:bg-linear-to-b data-vertical:from-transparent data-vertical:via-border data-vertical:to-transparent",
  solid: "",
};

export function AppSeparator({
  className,
  variant = "solid",
  ...props
}: AppSeparatorProps) {
  return (
    <Separator
      className={cn(separatorVariantClassName[variant], className)}
      {...props}
    />
  );
}
