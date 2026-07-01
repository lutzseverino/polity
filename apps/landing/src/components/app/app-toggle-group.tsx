import type { ComponentProps, ReactNode } from "react";

import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { cn } from "@/lib/utils";

type AppToggleGroupProps = Readonly<{
  "aria-label"?: string;
  "aria-labelledby"?: string;
  children: ReactNode;
  className?: string;
  onValueChange?: (value: string) => void;
  spacing?: 0 | 2;
  type: "single";
  value: string;
}>;
type AppToggleGroupItemProps = Readonly<
  ComponentProps<"button"> & {
    size?: "default" | "sm";
    treatment?: "default" | "choice";
    value: string;
    variant?: "default" | "outline";
  }
>;

type AppToggleChoiceContentProps = Readonly<{
  copy: ReactNode;
  label: ReactNode;
}>;

export function AppToggleGroup(props: AppToggleGroupProps) {
  return <ToggleGroup {...props} />;
}

export function AppToggleGroupItem({
  className,
  treatment = "default",
  variant,
  ...props
}: AppToggleGroupItemProps) {
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

export function AppToggleChoiceContent({
  copy,
  label,
}: AppToggleChoiceContentProps) {
  return (
    <span className="grid gap-1">
      <span className="min-w-0 break-words font-display text-lg">{label}</span>
      <span
        className="min-w-0 text-wrap text-xs leading-5 text-muted-foreground"
        data-slot="choice-copy"
      >
        {copy}
      </span>
    </span>
  );
}
