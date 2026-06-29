import { useGSAP } from "@gsap/react";
import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import type { RefObject } from "react";
import { useRef } from "react";

import type { OnboardingStep } from "./onboarding";

gsap.registerPlugin(useGSAP, ScrollTrigger);

function prefersReducedMotion() {
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

// Normalise any CSS colour (including oklch from the theme) to an sRGB string so
// GSAP interpolates within one colour space — mixing rgb→oklch yields garbage.
// Rasterising one pixel forces the conversion (Chrome's canvas round-trips oklch
// strings otherwise).
function toRgb(color: string): string {
  const ctx = document.createElement("canvas").getContext("2d");
  if (!ctx) {
    return color;
  }
  ctx.fillStyle = color;
  ctx.fillRect(0, 0, 1, 1);
  const [r, g, b] = ctx.getImageData(0, 0, 1, 1).data;
  return `rgb(${r}, ${g}, ${b})`;
}

/**
 * Plays the hero seal's entrance as a quick glow sweeping out from the centre:
 * each run fades in igniting in a flash colour that contrasts its region —
 * white on the red star, red on the bone rings — then cools to its resting
 * tone, the delay set by the run's radius so the glow ripples outward. Only
 * opacity and colour animate (the text is never touched, so the grid can't
 * shift); the inline colour is cleared on completion so the resting tone (its
 * class) governs, which also keeps StrictMode correct.
 */
function revealSeal() {
  const seal = document.querySelector<HTMLElement>(
    "[data-seal-layer] [data-ascii-seal]",
  );
  if (!seal) {
    return;
  }

  const runs = seal.querySelectorAll<HTMLElement>("[data-ascii-run]");
  const starRun = seal.querySelector<HTMLElement>(
    "[data-ascii-run].text-primary",
  );
  const ringRun = seal.querySelector<HTMLElement>(
    "[data-ascii-run].text-foreground",
  );
  // The two resting tones are read once (every run is one or the other) — not
  // per run, which previously meant a getComputedStyle + canvas readback for
  // each of ~200 cells and dragged the load. Rings flash the star's red; the
  // star flashes white.
  const red = starRun
    ? toRgb(getComputedStyle(starRun).color)
    : "rgb(234,40,47)";
  const bone = ringRun
    ? toRgb(getComputedStyle(ringRun).color)
    : "rgb(235,230,218)";
  const white = "rgb(255, 255, 255)";

  for (const run of runs) {
    const isStar = run.classList.contains("text-primary");
    const radius = Number(run.dataset.r ?? 0);

    gsap.fromTo(
      run,
      { opacity: 0, color: isStar ? white : red },
      {
        opacity: 1,
        color: isStar ? red : bone,
        duration: 0.4,
        delay: 0.1 + radius * 0.5,
        ease: "power2.out",
        clearProps: "color",
      },
    );
  }
}

/**
 * Orchestrates the landing-page motion as one "boot" of the constitutional
 * mainframe:
 *  - the status eyebrow and copy come up, the stencil headline drops in line by
 *    line, and the ASCII seal powers on from its core outward;
 *  - the scroll cue breathes; sections reveal in grouped, staggered beats;
 *  - the record ledger prints one line at a time as it enters view.
 *
 * Under prefers-reduced-motion nothing animates and the page renders static.
 * Entrances use fromTo() with explicit end states so they stay correct through
 * React StrictMode's mount/unmount/remount; useGSAP reverts on unmount.
 */
export function useLandingMotion(scope: RefObject<HTMLElement | null>) {
  useGSAP(
    () => {
      if (prefersReducedMotion()) {
        return;
      }

      const ease = "power3.out";

      // Boot sequence — headline lines, then the supporting copy.
      gsap
        .timeline({ defaults: { ease } })
        .fromTo(
          "[data-boot-line]",
          { yPercent: 118 },
          { yPercent: 0, duration: 0.9, stagger: 0.09 },
        )
        .fromTo(
          "[data-boot]",
          { y: 16, opacity: 0 },
          { y: 0, opacity: 1, duration: 0.7, stagger: 0.1 },
          "-=0.6",
        );

      // The seal fades and scales in, its rows igniting from the centre out.
      revealSeal();

      // The scroll cue breathes.
      gsap.to("[data-bob]", {
        y: 5,
        duration: 0.9,
        repeat: -1,
        yoyo: true,
        ease: "sine.inOut",
      });

      // Single elements fade up as they enter view.
      for (const el of gsap.utils.toArray<HTMLElement>("[data-reveal]")) {
        gsap.fromTo(
          el,
          { y: 26, opacity: 0 },
          {
            y: 0,
            opacity: 1,
            duration: 0.7,
            ease,
            scrollTrigger: { trigger: el, start: "top 86%" },
          },
        );
      }

      // Groups reveal their items as they enter view. A ledger prints its lines
      // from the left like a feed of paper; other groups simply rise.
      for (const group of gsap.utils.toArray<HTMLElement>(
        "[data-reveal-group]",
      )) {
        const lines = group.querySelectorAll("[data-ledger-line]");
        const isLedger = lines.length > 0;
        const items = isLedger
          ? lines
          : group.querySelectorAll("[data-reveal-item]");

        gsap.fromTo(
          items,
          isLedger ? { opacity: 0, x: -14 } : { opacity: 0, y: 30 },
          {
            opacity: 1,
            x: 0,
            y: 0,
            duration: isLedger ? 0.45 : 0.6,
            ease,
            stagger: isLedger ? 0.1 : 0.08,
            scrollTrigger: { trigger: group, start: "top 80%" },
          },
        );
      }
    },
    { scope },
  );
}

/**
 * Settles each onboarding step into place when the visitor moves between them.
 * Forward and back run the same quick fade-and-settle, so stepping in either
 * direction reads as one deliberate motion rather than an abrupt swap. The
 * naming boundary (step to/from null) is owned by the hero's seal/console
 * crossfade, so it is skipped here. Returns the ref for the step container.
 */
export function useStepTransition(step: OnboardingStep | null) {
  const stepRef = useRef<HTMLDivElement>(null);
  const previousStep = useRef(step);

  useGSAP(
    () => {
      const previous = previousStep.current;
      if (previous === step) {
        return;
      }
      previousStep.current = step;

      if (step === null || previous === null) {
        return;
      }

      if (prefersReducedMotion()) {
        return;
      }

      gsap.fromTo(
        stepRef.current,
        { opacity: 0, y: 10 },
        { opacity: 1, y: 0, duration: 0.34, ease: "power2.out" },
      );
    },
    { dependencies: [step], scope: stepRef },
  );

  return { stepRef };
}
