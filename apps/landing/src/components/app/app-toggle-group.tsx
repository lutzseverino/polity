import type { ComponentProps } from "react";

import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { cn } from "@/lib/utils";

export function AppToggleGroup(props: ComponentProps<typeof ToggleGroup>) {
  return <ToggleGroup {...props} />;
}

export function AppToggleGroupItem({
  className,
  treatment = "default",
  variant,
  ...props
}: ComponentProps<typeof ToggleGroupItem> & {
  treatment?: "default" | "choice";
}) {
  return (
    <ToggleGroupItem
      className={cn(
        treatment === "choice" &&
          "h-auto justify-start rounded-none border-2 p-4 text-left data-[state=on]:bg-primary data-[state=on]:text-primary-foreground data-[state=on]:[&_span[data-slot=choice-copy]]:text-primary-foreground/82",
        className,
      )}
      variant={treatment === "choice" ? "outline" : variant}
      {...props}
    />
  );
}
