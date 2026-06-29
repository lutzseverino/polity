import type { ComponentProps } from "react";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

type AppButtonSize = "default" | "icon-xs" | "sm";
type AppButtonTreatment = "default" | "plain";
type AppButtonVariant = "primary" | "secondary" | "ghost";

type AppButtonProps = Readonly<
  ComponentProps<"button"> & {
    size?: AppButtonSize;
    treatment?: AppButtonTreatment;
    variant?: AppButtonVariant;
  }
>;

export function AppButton({
  className,
  size = "default",
  treatment = "default",
  variant = "primary",
  ...props
}: AppButtonProps) {
  const buttonVariant = variant === "primary" ? "default" : variant;

  return (
    <Button
      className={cn(
        treatment === "default" &&
          "h-11 rounded-none px-5 font-mono text-xs leading-none tracking-[0.14em] uppercase",
        className,
      )}
      size={size}
      variant={buttonVariant}
      {...props}
    />
  );
}
