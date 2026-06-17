import { useGSAP } from "@gsap/react";
import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import type { RefObject } from "react";

gsap.registerPlugin(useGSAP, ScrollTrigger);

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
      const prefersReducedMotion = window.matchMedia(
        "(prefers-reduced-motion: reduce)",
      ).matches;

      if (prefersReducedMotion) {
        return;
      }

      const ease = "power3.out";

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
    },
    { scope },
  );
}
