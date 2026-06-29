import type { ComponentProps } from "react";

import { cn } from "@/lib/utils";

type TerminalRuleProps = Readonly<
  ComponentProps<"div"> & {
    label?: string;
    /** Right-aligned tail text, e.g. a section or record number. */
    tail?: string;
  }
>;

/**
 * A labelled divider drawn as a command-line rule: a marked tick, a caption, an
 * em-dash field, and an optional right-aligned code. Used to head sections and
 * panels the way a printout numbers its parts.
 */
export function TerminalRule({
  className,
  label,
  tail,
  ...props
}: TerminalRuleProps) {
  return (
    <div
      className={cn(
        "flex items-center gap-3 font-mono text-[0.7rem] leading-none tracking-[0.24em] text-muted-foreground uppercase",
        className,
      )}
      {...props}
    >
      <span aria-hidden="true" className="text-primary">
        ─■─
      </span>
      {label ? <span className="shrink-0 pt-px">{label}</span> : null}
      <span aria-hidden="true" className="h-px flex-1 bg-border" />
      {tail ? (
        <span className="shrink-0 pt-px text-muted-foreground/70">{tail}</span>
      ) : null}
    </div>
  );
}
