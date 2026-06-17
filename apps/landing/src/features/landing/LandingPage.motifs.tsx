import type { ComponentProps } from "react";

import { cn } from "@/lib/utils";

function starburstPoints(spikes: number, outer: number, inner: number) {
  const center = 100;
  const step = Math.PI / spikes;
  let angle = -Math.PI / 2;
  const points: string[] = [];

  for (let i = 0; i < spikes; i += 1) {
    points.push(
      `${center + Math.cos(angle) * outer},${center + Math.sin(angle) * outer}`,
    );
    angle += step;
    points.push(
      `${center + Math.cos(angle) * inner},${center + Math.sin(angle) * inner}`,
    );
    angle += step;
  }

  return points.join(" ");
}

/** Brutalist red explosion — the recurring star motif. */
export function Starburst({
  className,
  spikes = 12,
  inner = 0.52,
  ...props
}: ComponentProps<"svg"> & {
  spikes?: number;
  inner?: number;
}) {
  return (
    <svg
      aria-hidden="true"
      className={cn("text-primary", className)}
      fill="currentColor"
      preserveAspectRatio="xMidYMid meet"
      viewBox="0 0 200 200"
      xmlns="http://www.w3.org/2000/svg"
      {...props}
    >
      <polygon points={starburstPoints(spikes, 100, 100 * inner)} />
    </svg>
  );
}

/** Constructivist shards — three right triangles sharing one 45° diagonal. */
export function Shards({ className, ...props }: ComponentProps<"svg">) {
  return (
    <svg
      aria-hidden="true"
      className={cn("text-primary", className)}
      fill="currentColor"
      preserveAspectRatio="xMidYMid meet"
      viewBox="0 0 200 200"
      xmlns="http://www.w3.org/2000/svg"
      {...props}
    >
      <polygon points="0,0 124,0 0,124" />
      <polygon points="200,92 200,200 92,200" />
      <polygon points="150,0 200,0 200,50" />
    </svg>
  );
}

/** Official seal — monogram emblem rendered in currentColor. */
export function Emblem({ className, ...props }: ComponentProps<"svg">) {
  return (
    <svg
      aria-hidden="true"
      className={cn("text-current", className)}
      fill="none"
      viewBox="0 0 120 120"
      xmlns="http://www.w3.org/2000/svg"
      {...props}
    >
      <title>decreos seal</title>
      <circle cx="60" cy="60" r="58" stroke="currentColor" strokeWidth="1.4" />
      <circle
        cx="60"
        cy="60"
        fill="none"
        r="54"
        stroke="currentColor"
        strokeDasharray="0.6 5.2"
        strokeWidth="3"
      />
      <circle cx="60" cy="60" r="30" stroke="currentColor" strokeWidth="1.4" />

      <g className="origin-center motion-safe:[animation:spin_72s_linear_infinite]">
        <path d="M60 17 a43 43 0 1 1 -0.01 0" fill="none" id="emblem-band" />
        <text
          className="font-mono text-[9px] tracking-[0.16em] uppercase"
          fontWeight="500"
        >
          <textPath fill="currentColor" href="#emblem-band" startOffset="0">
            Decreos · MMXXVI · Decreos · MMXXVI ·
          </textPath>
        </text>
      </g>

      <text
        className="font-display"
        dominantBaseline="central"
        fill="currentColor"
        fontSize="19"
        textAnchor="middle"
        x="60"
        y="61"
      >
        DCR
      </text>
    </svg>
  );
}
