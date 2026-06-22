import type { ComponentProps, ReactNode } from "react";

import { cn } from "@/lib/utils";

type StepperStepState = "complete" | "current" | "upcoming";

type StepperProps = Readonly<ComponentProps<"ol">>;

type StepperStepProps = Readonly<
  Omit<ComponentProps<"button">, "children" | "type"> & {
    label: ReactNode;
    number: number;
    state: StepperStepState;
  }
>;

export function Stepper({ className, ...props }: StepperProps) {
  return (
    <ol
      className={cn("grid grid-cols-2 gap-2 sm:grid-cols-4", className)}
      {...props}
    />
  );
}

export function StepperStep({
  className,
  disabled,
  label,
  number,
  state,
  ...props
}: StepperStepProps) {
  const isActive = state === "complete" || state === "current";

  return (
    <li className="flex flex-col gap-1.5">
      <div
        className={cn(
          "h-1 transition-colors",
          isActive ? "bg-primary" : "bg-muted",
        )}
      />
      <button
        {...props}
        aria-current={state === "current" ? "step" : undefined}
        className={cn(
          "w-full text-left font-mono text-[0.65rem] leading-none tracking-[0.18em] uppercase transition-colors",
          state === "current" ? "text-foreground" : "text-muted-foreground",
          !disabled && "hover:text-foreground",
          disabled && "cursor-default",
          className,
        )}
        disabled={disabled}
        type="button"
      >
        {`${String(number).padStart(2, "0")} · `}
        {label}
      </button>
    </li>
  );
}
