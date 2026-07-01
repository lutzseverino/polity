import { X } from "lucide-react";
import type { ComponentProps } from "react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { AppButton } from "./app-button";

type AppBadgeVariant = "outline" | "secondary";

type AppBadgeProps = Readonly<
  ComponentProps<"span"> & {
    treatment?: "default" | "removable";
    variant?: AppBadgeVariant;
  }
>;

type BadgeRemoveButtonProps = Readonly<
  Omit<
    ComponentProps<typeof AppButton>,
    "children" | "size" | "treatment" | "variant"
  >
>;

export function AppBadge({
  className,
  treatment = "default",
  variant = "outline",
  ...props
}: AppBadgeProps) {
  return (
    <Badge
      className={cn(
        "h-8 rounded-none px-3 font-mono text-xs leading-none",
        treatment === "removable" && "max-w-full gap-2 pr-1",
        className,
      )}
      variant={variant}
      {...props}
    />
  );
}

export function BadgeRemoveButton({
  className,
  type = "button",
  ...props
}: BadgeRemoveButtonProps) {
  return (
    <AppButton
      className={cn("rounded-none", className)}
      data-slot="badge-remove"
      size="icon-xs"
      treatment="plain"
      type={type}
      variant="secondary"
      {...props}
    >
      <X aria-hidden="true" />
    </AppButton>
  );
}
