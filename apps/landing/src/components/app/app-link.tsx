import type { ComponentProps } from "react";

import { cn } from "@/lib/utils";

export function AppLink({ className, ...props }: ComponentProps<"a">) {
  return (
    <a
      className={cn(
        "text-foreground underline-offset-4 hover:text-primary hover:underline",
        className,
      )}
      {...props}
    />
  );
}
