import type { ComponentProps } from "react";

import { cn } from "@/lib/utils";

type AppLinkProps = Readonly<ComponentProps<"a">>;

export function AppLink({ className, ...props }: AppLinkProps) {
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
