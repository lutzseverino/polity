import type { ComponentProps } from "react";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

type AppButtonProps = Readonly<ComponentProps<typeof Button>>;

export function AppButton({ className, ...props }: AppButtonProps) {
  return (
    <Button
      className={cn(
        "h-11 rounded-none px-5 font-mono text-xs leading-none tracking-[0.14em] uppercase",
        className,
      )}
      {...props}
    />
  );
}
