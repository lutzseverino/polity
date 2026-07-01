import type { ComponentProps } from "react";
import { useEffect, useRef } from "react";

import { cn } from "@/lib/utils";
import {
  SEAL_CELL_ASPECT,
  SEAL_HEIGHT,
  SEAL_LINES,
  SEAL_WIDTH,
  sealToneAt,
} from "./seal-data";

type SealFieldProps = Readonly<
  Omit<ComponentProps<"div">, "children"> & {
    active?: boolean;
  }
>;

type Tone = "field" | "ring" | "star";
type RGBA = [number, number, number, number];
type Cell = { col: number; row: number; r: number; tone: Tone; char: string };

// A fixed, even wall of characters that fills the whole pane, with the seal
// struck dead centre. Drawn to a <canvas> rather than the DOM: recolouring
// thousands of glyphs every frame is paint-bound on elements, but a single
// canvas draw loop holds framerate. Width/height suit a desktop hero pane; the
// font size is solved at run time to cover whatever pane it lands in.
const FIELD_WIDTH = 84;
const FIELD_HEIGHT = 64;
const OFFSET_X = Math.round((FIELD_WIDTH - SEAL_WIDTH) / 2);
const OFFSET_Y = Math.round((FIELD_HEIGHT - SEAL_HEIGHT) / 2);
const CX = (FIELD_WIDTH - 1) / 2;
const CY = (FIELD_HEIGHT - 1) / 2;
const MAX_R = Math.hypot(CX * SEAL_CELL_ASPECT, CY);

// The teletype alphabet the background drifts through — terminal punctuation and
// binary, nothing that reads as a word.
const GLYPHS = "01<>/\\|=+*-:.#%@";
// The reveal: how long the wave takes to cross the field, and how wide (in
// normalised radius) the hot band riding its front is.
const REVEAL_MS = 1900;
const BAND = 0.2;
// The settled drift: cells reshuffled per tick, and how often a tick lands.
const SCRAMBLE_PER_TICK = 22;
const SCRAMBLE_INTERVAL_MS = 90;

function randomGlyph() {
  return GLYPHS[Math.floor(Math.random() * GLYPHS.length)] ?? ".";
}

function prefersReducedMotion() {
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function buildCells(): Cell[] {
  const cells: Cell[] = [];
  for (let row = 0; row < FIELD_HEIGHT; row += 1) {
    for (let col = 0; col < FIELD_WIDTH; col += 1) {
      const sealX = col - OFFSET_X;
      const sealY = row - OFFSET_Y;
      const line = SEAL_LINES[sealY] ?? "";
      const ch = sealX >= 0 && sealX < SEAL_WIDTH ? (line[sealX] ?? " ") : " ";
      const tone = sealToneAt(ch, sealX, sealY);
      cells.push({
        col,
        row,
        r: Math.hypot((col - CX) * SEAL_CELL_ASPECT, row - CY) / MAX_R,
        tone: tone === "space" ? "field" : tone,
        char: tone === "space" ? randomGlyph() : ch,
      });
    }
  }
  return cells;
}

// Resolve any CSS colour (including the theme's oklch) to rgba by painting one
// pixel and reading it back — the only reliable cross-space conversion.
function makeColorReader() {
  const probe = document.createElement("canvas");
  probe.width = 1;
  probe.height = 1;
  const ctx = probe.getContext("2d", { willReadFrequently: true });
  return (value: string, fallback: RGBA, alpha = 1): RGBA => {
    const trimmed = value.trim();
    if (!ctx || !trimmed) {
      return [fallback[0], fallback[1], fallback[2], alpha];
    }
    ctx.clearRect(0, 0, 1, 1);
    ctx.fillStyle = "#000";
    ctx.fillStyle = trimmed;
    ctx.fillRect(0, 0, 1, 1);
    const [r, g, b] = ctx.getImageData(0, 0, 1, 1).data;
    return [r, g, b, alpha];
  };
}

const lerp = (a: RGBA, b: RGBA, t: number): RGBA => [
  a[0] + (b[0] - a[0]) * t,
  a[1] + (b[1] - a[1]) * t,
  a[2] + (b[2] - a[2]) * t,
  a[3] + (b[3] - a[3]) * t,
];

const toCss = (c: RGBA) =>
  `rgba(${c[0] | 0}, ${c[1] | 0}, ${c[2] | 0}, ${c[3]})`;

/**
 * The hero's resting state: the constitutional seal struck into a full-bleed
 * wall of teletype characters rather than framed on a plate. On load the
 * mainframe powers on as one pulse — a wave of light rolls out from the seal's
 * core, each character appearing the instant the wave reaches it, flaring hot
 * (the field red, the seal white) and cooling to its true tone behind. The
 * reveal *is* the pulse; it fires once. Afterwards the background glyphs drift
 * in place and only those cells repaint, so the steady state is nearly free.
 *
 * Rendered to a canvas so recolouring thousands of glyphs holds framerate.
 * Decorative; hidden from assistive tech, and static under reduced motion.
 */
export function SealField({
  active = true,
  className,
  ...props
}: SealFieldProps) {
  const wrapRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const wrap = wrapRef.current;
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext("2d");
    if (!wrap || !canvas || !ctx) {
      return;
    }

    const cells = buildCells();
    const fieldCells = cells.filter((cell) => cell.tone === "field");

    const read = makeColorReader();
    const fontFamily =
      getComputedStyle(wrap).getPropertyValue("--font-mono").trim() ||
      "monospace";
    const white: RGBA = [255, 255, 255, 1];
    let red: RGBA = [234, 40, 47, 1];
    let bone: RGBA = [235, 230, 218, 1];
    let faint: RGBA = [150, 150, 140, 0.25];

    // Pull the theme's tones from the cascade. The field mounts before
    // ThemeProvider applies the theme class (child effects run before parent
    // ones) and the palette changes again whenever the visitor flips the theme,
    // so colours are read here and refreshed on every <html> class change —
    // otherwise the canvas keeps painting the palette it first happened to see.
    const readColours = () => {
      const styles = getComputedStyle(wrap);
      red = read(styles.getPropertyValue("--primary"), red);
      bone = read(styles.getPropertyValue("--foreground"), bone);
      const muted = read(styles.getPropertyValue("--muted-foreground"), [
        faint[0],
        faint[1],
        faint[2],
        1,
      ]);
      faint = [muted[0], muted[1], muted[2], 0.25];
    };
    readColours();

    const restColour = (cell: Cell): RGBA =>
      cell.tone === "star" ? red : cell.tone === "ring" ? bone : faint;
    const hotColour = (cell: Cell): RGBA =>
      cell.tone === "field" ? red : white;

    let paneW = 0;
    let paneH = 0;
    let cellW = 0;
    let cellH = 0;
    let offX = 0;
    let offY = 0;

    const layout = () => {
      const rect = wrap.getBoundingClientRect();
      paneW = rect.width;
      paneH = rect.height;
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      // Solve the font size that covers the pane (the longer axis bleeds off and
      // is clipped), so the wall always reaches every edge.
      const fontPx = Math.max(
        paneW / (FIELD_WIDTH * SEAL_CELL_ASPECT),
        paneH / FIELD_HEIGHT,
      );
      cellH = fontPx;
      cellW = fontPx * SEAL_CELL_ASPECT;
      offX = (paneW - FIELD_WIDTH * cellW) / 2;
      offY = (paneH - FIELD_HEIGHT * cellH) / 2;
      canvas.width = Math.round(paneW * dpr);
      canvas.height = Math.round(paneH * dpr);
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      ctx.font = `${fontPx}px ${fontFamily}`;
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
    };

    const paint = (cell: Cell, colour: RGBA) => {
      ctx.fillStyle = toCss(colour);
      ctx.fillText(
        cell.char,
        offX + cell.col * cellW + cellW / 2,
        offY + cell.row * cellH + cellH / 2,
      );
    };

    const drawRest = () => {
      ctx.clearRect(0, 0, paneW, paneH);
      for (const cell of cells) {
        paint(cell, restColour(cell));
      }
    };

    let raf = 0;
    let scrambleTimer = 0;
    let done = false;

    const drift = () => {
      scrambleTimer = window.setInterval(() => {
        for (let i = 0; i < SCRAMBLE_PER_TICK; i += 1) {
          const cell =
            fieldCells[Math.floor(Math.random() * fieldCells.length)];
          if (!cell) {
            continue;
          }
          cell.char = randomGlyph();
          ctx.clearRect(
            offX + cell.col * cellW,
            offY + cell.row * cellH,
            cellW + 1,
            cellH + 1,
          );
          paint(cell, restColour(cell));
        }
      }, SCRAMBLE_INTERVAL_MS);
    };

    let start = 0;
    const frame = (now: number) => {
      if (!start) {
        start = now;
      }
      const linear = Math.min(1, (now - start) / REVEAL_MS);
      const eased = 1 - (1 - linear) ** 2; // power2.out — the wave decelerates
      const front = eased * (1 + BAND);

      ctx.clearRect(0, 0, paneW, paneH);
      for (const cell of cells) {
        const d = front - cell.r;
        if (d <= 0) {
          continue; // the wave hasn't reached this glyph yet
        }
        paint(
          cell,
          d < BAND
            ? lerp(hotColour(cell), restColour(cell), d / BAND)
            : restColour(cell),
        );
      }

      if (linear < 1) {
        raf = requestAnimationFrame(frame);
      } else {
        done = true;
        drawRest();
        drift();
      }
    };

    const observer = new ResizeObserver(() => {
      layout();
      if (done || prefersReducedMotion()) {
        drawRest();
      }
    });
    observer.observe(wrap);

    // Refresh the palette when the theme flips. Mid-reveal the draw loop already
    // reads the live colours; once settled, repaint the static field.
    const themeObserver = new MutationObserver(() => {
      readColours();
      if (done) {
        drawRest();
      }
    });
    themeObserver.observe(document.documentElement, {
      attributeFilter: ["class"],
    });

    layout();
    if (!active || prefersReducedMotion()) {
      done = true;
      drawRest();
    } else {
      raf = requestAnimationFrame(frame);
    }

    return () => {
      cancelAnimationFrame(raf);
      window.clearInterval(scrambleTimer);
      themeObserver.disconnect();
      observer.disconnect();
    };
  }, [active]);

  return (
    <div
      aria-hidden="true"
      className={cn("relative overflow-hidden", className)}
      data-seal-field
      ref={wrapRef}
      {...props}
    >
      <canvas
        className="pointer-events-none absolute inset-0 block h-full w-full"
        ref={canvasRef}
      />
    </div>
  );
}
