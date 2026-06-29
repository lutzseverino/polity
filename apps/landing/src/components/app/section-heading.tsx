import type { ReactNode } from "react";

import { cn } from "@/lib/utils";

import { TerminalRule } from "./terminal-rule";

type SectionHeadingProps = Readonly<{
  className?: string;
  /** The section's eyebrow label, set in the rule. */
  eyebrow: string;
  /** Right-aligned section code shown in the rule, e.g. "§ 02". */
  index?: string;
  /** The display headline; pass nodes for line breaks or an emphasis span. */
  children: ReactNode;
  /** Optional lead paragraph under the headline. */
  lead?: ReactNode;
}>;

/**
 * A section's opening: a labelled rule, a stencil headline, and an optional lead
 * paragraph — the stacked intro shared by the record and motion sections (and
 * available to the dashboard). Each part carries data-reveal so the page motion
 * stages them in. The headline is passed as children so a section can break it
 * across lines or colour an emphasis span.
 */
export function SectionHeading({
  className,
  eyebrow,
  index,
  children,
  lead,
}: SectionHeadingProps) {
  return (
    <div className={cn("flex flex-col gap-6", className)}>
      <TerminalRule data-reveal label={eyebrow} tail={index} />
      <h2
        className="font-display text-[clamp(2.25rem,5vw,4.5rem)] leading-[0.84]"
        data-reveal
      >
        {children}
      </h2>
      {lead ? (
        <p className="max-w-prose leading-7 text-muted-foreground" data-reveal>
          {lead}
        </p>
      ) : null}
    </div>
  );
}
