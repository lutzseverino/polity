import type { ComponentProps, ReactNode } from "react";

import { cn } from "@/lib/utils";
import { AppCard } from "./app-card";

type TerminalPanelProps = Readonly<ComponentProps<"div">>;

type TerminalPanelHeaderProps = Readonly<
  Omit<ComponentProps<"div">, "children"> & {
    /** Leading content (a label, often with a status dot). */
    start: ReactNode;
    /** Trailing content (a number, status, or count). */
    end?: ReactNode;
    /** Tint the bar to set it apart from the body. Defaults to true. */
    muted?: boolean;
  }
>;

/**
 * A bordered surface styled like a terminal window — the shared shell behind the
 * hero seal plate, the founding console, the record ledger, and the motion card.
 * It only owns the frame; callers compose the header, body, and footer inside.
 */
export function TerminalPanel({ className, ...props }: TerminalPanelProps) {
  return (
    <AppCard className={cn("border", className)} padding="none" {...props} />
  );
}

/**
 * The title bar of a TerminalPanel: a mono, uppercase rule with leading and
 * trailing slots. Centralises the bar styling that every panel shared.
 */
export function TerminalPanelHeader({
  className,
  start,
  end,
  muted = true,
  ...props
}: TerminalPanelHeaderProps) {
  return (
    <div
      className={cn(
        "flex items-center justify-between gap-3 border-b px-3 py-2.5 font-mono text-[0.62rem] leading-none tracking-[0.24em] text-muted-foreground uppercase",
        muted && "bg-muted/40",
        className,
      )}
      {...props}
    >
      <span className="inline-flex items-center gap-2">{start}</span>
      {end ? (
        <span className="inline-flex items-center gap-2">{end}</span>
      ) : null}
    </div>
  );
}

type TerminalDotProps = Readonly<{ pulse?: boolean }>;

/** The small square status marker used in panel headers and labels. */
export function TerminalDot({ pulse = false }: TerminalDotProps) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        "block size-1.5 bg-primary",
        pulse && "motion-safe:animate-pulse",
      )}
    />
  );
}
