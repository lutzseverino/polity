import type { ComponentProps } from "react";

import { ToggleGroupItem } from "@/components/ui/toggle-group";
import { cn } from "@/lib/utils";

export function ChoiceToggleGroupItem({
  className,
  ...props
}: ComponentProps<typeof ToggleGroupItem>) {
  return (
    <ToggleGroupItem
      className={cn(
        "h-auto justify-start rounded-none border-2 p-4 text-left data-[state=on]:bg-primary data-[state=on]:text-primary-foreground data-[state=on]:[&_span[data-slot=choice-copy]]:text-primary-foreground/82",
        className,
      )}
      variant="outline"
      {...props}
    />
  );
}
