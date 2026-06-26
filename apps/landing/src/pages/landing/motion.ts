import { useGSAP } from "@gsap/react";
import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import type { RefObject } from "react";
import { useCallback, useRef } from "react";

import type { OnboardingStep } from "./onboarding";

gsap.registerPlugin(useGSAP, ScrollTrigger);

const STAR_DIP_AMOUNT = 0.44;
const STAR_DIP_TAPER = 0.18;
const FOUNDING_TRANSITION = {
  incomingFadeDuration: 0.24,
  outgoingFadeDuration: 0.18,
  posterDuration: 0.95,
  posterEase: "power2.inOut",
} as const;
const foundingSelectors = {
  burstShell: "[data-hero-burst-shell]",
  onboarding: "[data-onboarding-surface]",
  plate: "[data-record-plate-surface]",
  stage: "[data-hero-stage]",
  visual: "[data-founding-visual]",
} as const;

function prefersReducedMotion() {
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function restoreNativeScrollRestoration() {
  if ("scrollRestoration" in window.history) {
    window.history.scrollRestoration = "auto";
  }
}

function interpolate(start: number, end: number, progress: number) {
  return start + (end - start) * progress;
}

function smoothstep(progress: number) {
  return progress * progress * (3 - 2 * progress);
}

function foundingStarScale(
  progress: number,
  startScale: number,
  targetScale: number,
) {
  const curvedProgress = smoothstep(progress);
  const growth = interpolate(startScale, targetScale, curvedProgress);
  const dip =
    Math.sin(Math.PI * curvedProgress) *
    STAR_DIP_AMOUNT *
    (1 - progress) ** STAR_DIP_TAPER;

  return growth - dip;
}

type FoundingVisualState = {
  clipPath: string;
  scale: number;
  x: number;
  y: number;
};

type FoundingElements = {
  burstShell: HTMLElement | null;
  onboarding: HTMLElement | null;
  plate: HTMLElement | null;
  stage: HTMLElement | null;
  visual: HTMLElement | null;
};

function getFoundingElements(section: HTMLElement): FoundingElements {
  return {
    burstShell: section.querySelector<HTMLElement>(
      foundingSelectors.burstShell,
    ),
    onboarding: section.querySelector<HTMLElement>(
      foundingSelectors.onboarding,
    ),
    plate: section.querySelector<HTMLElement>(foundingSelectors.plate),
    stage: section.querySelector<HTMLElement>(foundingSelectors.stage),
    visual: section.querySelector<HTMLElement>(foundingSelectors.visual),
  };
}

function present<T>(value: T | null): value is T {
  return value !== null;
}

function foundingVisualTarget(section: HTMLElement, founding: boolean) {
  if (founding) {
    return section;
  }
  return getFoundingElements(section).stage ?? section;
}

function measureFoundingVisual(
  section: HTMLElement,
  founding: boolean,
): FoundingVisualState {
  const target = foundingVisualTarget(section, founding);
  // The clip-path and burst translate are applied to the founding visual, so
  // every inset must be measured in *its* box. The visual is `absolute inset-0`
  // within the section's padding box — inside the section's border. Measuring
  // against the section's border box instead over-clips by the border width and
  // leaves a hairline of background along the bottom edge before the separator.
  const { visual } = getFoundingElements(section);
  const frameBox = (visual ?? section).getBoundingClientRect();
  const targetBox = target.getBoundingClientRect();
  const top = Math.max(0, targetBox.top - frameBox.top);
  const right = Math.max(0, frameBox.right - targetBox.right);
  const bottom = Math.max(0, frameBox.bottom - targetBox.bottom);
  const left = Math.max(0, targetBox.left - frameBox.left);
  const clippedTargetLeft = frameBox.left + left;
  const clippedTargetRight = frameBox.right - right;
  const clippedTargetTop = frameBox.top + top;
  const clippedTargetBottom = frameBox.bottom - bottom;
  const targetCenterX =
    clippedTargetLeft + (clippedTargetRight - clippedTargetLeft) / 2;
  const targetCenterY =
    clippedTargetTop + (clippedTargetBottom - clippedTargetTop) / 2;
  const frameCenterX = frameBox.left + frameBox.width / 2;
  const frameCenterY = frameBox.top + frameBox.height / 2;

  return {
    clipPath: `inset(${top}px ${right}px ${bottom}px ${left}px)`,
    scale: founding ? 1.5 : 1,
    x: targetCenterX - frameCenterX,
    y: targetCenterY - frameCenterY,
  };
}

function applyFoundingVisual(section: HTMLElement, state: FoundingVisualState) {
  const { burstShell, visual } = getFoundingElements(section);

  if (visual) {
    gsap.set(visual, { clipPath: state.clipPath });
  }
  if (burstShell) {
    gsap.set(burstShell, {
      scale: state.scale,
      transformOrigin: "50% 50%",
      x: state.x,
      y: state.y,
    });
  }
}

function applyFoundingSurfaces(section: HTMLElement, founding: boolean) {
  const { onboarding, plate } = getFoundingElements(section);

  gsap.set(plate, { autoAlpha: founding ? 0 : 1 });
  gsap.set(onboarding, { autoAlpha: founding ? 1 : 0 });
}

function foundingAnimationTargets(section: HTMLElement) {
  const { burstShell, onboarding, plate, visual } =
    getFoundingElements(section);

  return [visual, burstShell, plate, onboarding].filter(present);
}

function syncFoundingRestingState(section: HTMLElement, founding: boolean) {
  const { onboarding, plate } = getFoundingElements(section);

  gsap.set([plate, onboarding].filter(present), { clearProps: "transform" });
  applyFoundingVisual(section, measureFoundingVisual(section, founding));
  applyFoundingSurfaces(section, founding);
}

function pinToPreviousVisualPosition(
  element: HTMLElement | null,
  fromVisual: FoundingVisualState,
  toVisual: FoundingVisualState,
) {
  if (!element) {
    return;
  }
  gsap.set(element, {
    x: fromVisual.x - toVisual.x,
    y: fromVisual.y - toVisual.y,
  });
}

/**
 * Orchestrates the landing-page motion:
 *  - a staggered hero reveal that ends with the seal "stamping" onto the record
 *  - the centre starburst turning slowly, decorative bursts drifting on scroll
 *  - section content revealing in grouped, staggered beats as it enters view
 *
 * Under prefers-reduced-motion nothing animates and the layout renders static.
 * Entrances use fromTo() with explicit end states so they stay correct through
 * React StrictMode's mount/unmount/remount; useGSAP reverts everything on unmount.
 */
export function useLandingMotion(scope: RefObject<HTMLElement | null>) {
  useGSAP(
    () => {
      if (prefersReducedMotion()) {
        return;
      }

      restoreNativeScrollRestoration();
      const ease = "power3.out";
      const restoreScrollRestoration = gsap.delayedCall(
        0.65,
        restoreNativeScrollRestoration,
      );

      // Hero entrance — headline drops in, the record assembles, the seal lands.
      gsap
        .timeline({ defaults: { ease } })
        .fromTo(
          "[data-hero-line]",
          { yPercent: 115 },
          { yPercent: 0, duration: 0.9, stagger: 0.08 },
        )
        .fromTo(
          "[data-hero-fade]",
          { y: 18, opacity: 0 },
          { y: 0, opacity: 1, duration: 0.7, stagger: 0.1 },
          "-=0.55",
        )
        .fromTo(
          "[data-hero-plate]",
          { scale: 0.92, opacity: 0 },
          { scale: 1, opacity: 1, duration: 0.8 },
          "-=0.85",
        )
        .fromTo(
          "[data-hero-burst]",
          { scale: 0.4, opacity: 0 },
          { scale: 1, opacity: 1, duration: 1.1, ease: "back.out(1.4)" },
          "<",
        )
        .fromTo(
          "[data-hero-mark]",
          { opacity: 0 },
          { opacity: 1, duration: 0.5, stagger: 0.06 },
          "-=0.5",
        )
        .fromTo(
          "[data-hero-stamp]",
          { scale: 1.65, rotate: -14, opacity: 0 },
          {
            scale: 1,
            rotate: 0,
            opacity: 1,
            duration: 0.55,
            ease: "back.out(2.4)",
          },
          "-=0.15",
        );

      // The centre starburst turns slowly and forever.
      gsap.to("[data-hero-burst]", {
        rotate: 360,
        duration: 90,
        repeat: -1,
        ease: "none",
      });

      // The scroll cue breathes.
      gsap.to("[data-bob]", {
        y: 5,
        duration: 0.9,
        repeat: -1,
        yoyo: true,
        ease: "sine.inOut",
      });

      // Decorative bursts turn as their section scrolls past.
      for (const el of gsap.utils.toArray<HTMLElement>("[data-scrub]")) {
        gsap.fromTo(
          el,
          { rotate: -28 },
          {
            rotate: 32,
            ease: "none",
            scrollTrigger: {
              trigger: el,
              start: "top bottom",
              end: "bottom top",
              scrub: 1,
            },
          },
        );
      }

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

      // Groups reveal their items in a staggered sequence.
      for (const group of gsap.utils.toArray<HTMLElement>(
        "[data-reveal-group]",
      )) {
        gsap.fromTo(
          group.querySelectorAll("[data-reveal-item]"),
          { y: 30, opacity: 0 },
          {
            y: 0,
            opacity: 1,
            duration: 0.6,
            ease,
            stagger: 0.08,
            scrollTrigger: { trigger: group, start: "top 80%" },
          },
        );
      }

      return () => restoreScrollRestoration.kill();
    },
    { scope },
  );
}

/**
 * Choreographs the founding hand-off as a reversible timeline. Calling
 * capture() just before the founding state flips records the poster geometry;
 * the layout effect then plays one timeline:
 *  - the dark ground unfolds from the record panel to a full-bleed section fill
 *    using a measured clip path on the stable section-level visual layer,
 *  - the active card fades in place while the poster starts moving, and the
 *    next card fades in only after the poster settles, and
 *  - the starburst glides to centre and swells.
 *
 * The title/input layer stays mounted underneath the poster, so the expanding
 * ground physically covers it on the way in and reveals it on the way back.
 */
export function useFoundingTransition(founding: boolean) {
  const sectionRef = useRef<HTMLElement>(null);
  const visualFrom = useRef<FoundingVisualState | null>(null);

  const capture = useCallback(() => {
    const section = sectionRef.current;
    if (!section || prefersReducedMotion()) {
      return;
    }
    visualFrom.current = measureFoundingVisual(section, founding);
  }, [founding]);

  useGSAP(
    () => {
      const section = sectionRef.current;
      if (!section) {
        return;
      }
      let cancelled = false;
      let frame: number | null = null;
      const requestSyncRestingState = () => {
        if (cancelled) {
          return;
        }
        if (frame !== null) {
          window.cancelAnimationFrame(frame);
        }
        frame = window.requestAnimationFrame(() => {
          frame = null;
          if (!cancelled) {
            syncFoundingRestingState(section, founding);
          }
        });
      };
      const syncAfterGeometryChange = () => {
        gsap.killTweensOf(foundingAnimationTargets(section));
        requestSyncRestingState();
      };

      window.addEventListener("resize", syncAfterGeometryChange);

      const fromVisual = visualFrom.current;
      if (!fromVisual) {
        frame = window.requestAnimationFrame(() => {
          frame = null;
          if (document.fonts.status === "loaded") {
            syncFoundingRestingState(section, founding);
          }
        });
        applyFoundingSurfaces(section, founding);
        void document.fonts.ready.then(() => {
          requestSyncRestingState();
        });
        return () => {
          cancelled = true;
          if (frame !== null) {
            window.cancelAnimationFrame(frame);
          }
          window.removeEventListener("resize", syncAfterGeometryChange);
        };
      }
      visualFrom.current = null;
      const toVisual = measureFoundingVisual(section, founding);
      const { burstShell, onboarding, plate, visual } =
        getFoundingElements(section);

      const outgoingSurface = founding ? plate : onboarding;
      const incomingSurface = founding ? onboarding : plate;

      gsap.killTweensOf(foundingAnimationTargets(section));
      applyFoundingVisual(section, fromVisual);
      gsap.set(plate, { autoAlpha: founding ? 1 : 0 });
      gsap.set(onboarding, { autoAlpha: founding ? 0 : 1 });
      // The incoming surface fades in at its resting centre. Strip any inline
      // transform a previous crossing pinned on it (a back crossing pins the
      // onboarding card, and revertOnUpdate can resurrect that transform) so it
      // cannot flash in from the stale pin position. Clearing — rather than
      // setting x/y to 0 — is essential: the onboarding card is centred with a
      // percentage translate from CSS, which an explicit GSAP transform would
      // overwrite, dropping it half its size off-centre until it settled.
      gsap.set(incomingSurface, { clearProps: "transform" });
      pinToPreviousVisualPosition(outgoingSurface, fromVisual, toVisual);

      const tl = gsap.timeline({
        onComplete: () => {
          syncFoundingRestingState(section, founding);
        },
      });
      if (visual) {
        tl.to(
          visual,
          {
            clipPath: toVisual.clipPath,
            duration: FOUNDING_TRANSITION.posterDuration,
            ease: FOUNDING_TRANSITION.posterEase,
          },
          0,
        );
      }
      if (burstShell) {
        const starMotion = { progress: 0 };
        const moveEase = gsap.parseEase(FOUNDING_TRANSITION.posterEase);

        tl.to(
          starMotion,
          {
            duration: FOUNDING_TRANSITION.posterDuration,
            ease: "none",
            onUpdate: () => {
              const progress = starMotion.progress;
              const moved = moveEase(progress);

              gsap.set(burstShell, {
                scale: founding
                  ? foundingStarScale(
                      progress,
                      fromVisual.scale,
                      toVisual.scale,
                    )
                  : interpolate(fromVisual.scale, toVisual.scale, moved),
                x: interpolate(fromVisual.x, toVisual.x, moved),
                y: interpolate(fromVisual.y, toVisual.y, moved),
              });
            },
            progress: 1,
          },
          0,
        );
      }
      if (founding) {
        tl.to(
          plate,
          {
            autoAlpha: 0,
            duration: FOUNDING_TRANSITION.outgoingFadeDuration,
            ease: "power2.out",
          },
          0,
        ).to(
          onboarding,
          {
            autoAlpha: 1,
            duration: FOUNDING_TRANSITION.incomingFadeDuration,
            ease: "power2.out",
          },
          FOUNDING_TRANSITION.posterDuration,
        );
      } else {
        tl.to(
          onboarding,
          {
            autoAlpha: 0,
            duration: FOUNDING_TRANSITION.outgoingFadeDuration,
            ease: "power2.out",
          },
          0,
        ).to(
          plate,
          {
            autoAlpha: 1,
            duration: FOUNDING_TRANSITION.incomingFadeDuration,
            ease: "power2.out",
          },
          FOUNDING_TRANSITION.posterDuration,
        );
      }
      return () => {
        cancelled = true;
        if (frame !== null) {
          window.cancelAnimationFrame(frame);
        }
        window.removeEventListener("resize", syncAfterGeometryChange);
        tl.kill();
      };
    },
    { dependencies: [founding], revertOnUpdate: true, scope: sectionRef },
  );

  return { sectionRef, capture };
}

/**
 * Settles each onboarding step into place when the visitor moves between them.
 * Forward and back run the same quick fade-and-settle, so stepping in either
 * direction reads as one deliberate motion rather than an abrupt swap.
 *
 * Crossing the naming boundary (step to/from null) is left to
 * useFoundingTransition.
 * Returns the ref to attach to the step container.
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

      // The plate/instrument morph owns the naming boundary.
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
