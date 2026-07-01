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

      // (The seal field owns its own canvas power-on pulse, and the paper grain
      // scrolls with the page as an absolute layer — no JS needed for either.)

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
