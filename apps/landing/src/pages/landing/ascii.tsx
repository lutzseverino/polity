import type { ComponentProps } from "react";

import { cn } from "@/lib/utils";
import {
  SEAL_LINES,
  SEAL_WIDTH,
  type SealTone,
  sealRadiusAt,
  sealToneAt,
} from "./seal-data";

/** A contiguous run of identical-tone glyphs on one row. */
type SealRun = {
  /** Normalised radius of the run's centre, for centre-out motion. */
  r: number;
  text: string;
  tone: SealTone;
};

type AsciiSealProps = Readonly<
  Omit<ComponentProps<"pre">, "children"> & {
    /** Render every glyph in the inherited colour (for faint watermarks). */
    monochrome?: boolean;
  }
>;

const toneClass: Record<Exclude<SealTone, "space">, string> = {
  ring: "text-foreground",
  star: "text-primary",
};

// Group each row into contiguous same-tone runs once, at module load. Plain
// monospace text per run keeps the seal crisply aligned and immune to motion;
// each run records its centre radius so the reveal can ripple out from the core.
const SEAL_ROWS: SealRun[][] = SEAL_LINES.map((line, y) => {
  const padded = line.padEnd(SEAL_WIDTH, " ");
  const runs: Array<SealRun & { startX: number }> = [];
  for (let x = 0; x < padded.length; x += 1) {
    const ch = padded[x] ?? " ";
    const tone = sealToneAt(ch, x, y);
    const last = runs.at(-1);
    if (last && last.tone === tone) {
      last.text += ch;
    } else {
      runs.push({ r: 0, startX: x, text: ch, tone });
    }
  }
  return runs.map(({ startX, ...run }) => ({
    ...run,
    r: sealRadiusAt(startX + (run.text.length - 1) / 2, y),
  }));
});

/**
 * The signature element: the constitutional seal, struck in ASCII. The art is a
 * clean SVG seal rasterised to a luminance ramp (see seal-data.ts), rendered at
 * one resolution everywhere and sized by font-size, so it stays identical and
 * crisp from a hero plate down to a footer stamp. Decorative — hidden from
 * assistive tech. motion.ts reveals the hero instance with opacity and scale
 * only (rows carry [data-ascii-row]); it never touches the text.
 */
export function AsciiSeal({
  className,
  monochrome = false,
  ...props
}: AsciiSealProps) {
  return (
    <pre
      aria-hidden="true"
      className={cn("ascii text-[clamp(0.4rem,1.5vw,0.72rem)]", className)}
      data-ascii-seal
      {...props}
    >
      {SEAL_ROWS.map((runs, y) => (
        // biome-ignore lint/suspicious/noArrayIndexKey: fixed raster grid
        <div className="block" data-ascii-row key={y}>
          {runs.map((run, i) =>
            run.tone === "space" ? (
              // biome-ignore lint/suspicious/noArrayIndexKey: fixed raster grid
              <span key={i}>{run.text}</span>
            ) : (
              <span
                className={monochrome ? undefined : toneClass[run.tone]}
                data-ascii-run
                data-r={run.r.toFixed(3)}
                // biome-ignore lint/suspicious/noArrayIndexKey: fixed raster grid
                key={i}
              >
                {run.text}
              </span>
            ),
          )}
        </div>
      ))}
    </pre>
  );
}
