import { useRef } from "react";
import { LandingMasthead } from "./masthead";
import { useLandingMotion } from "./motion";
import { useLandingOnboarding } from "./onboarding";
import {
  Colophon,
  LandingHero,
  MethodSection,
  RecordSection,
  SocialSection,
} from "./sections";

export function LandingPage() {
  const onboarding = useLandingOnboarding();
  const scope = useRef<HTMLElement>(null);

  useLandingMotion(scope);

  return (
    <main
      className="relative min-h-[100dvh] bg-background pt-12 text-foreground [overflow-anchor:none]"
      data-landing-page
      ref={scope}
    >
      {/* Printout atmosphere. Scanlines, edge burn, and grain sit on the page
          plane rather than the viewport, so the texture scrolls with the
          content like material running under the type. All click-through, above
          content. */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 z-40 opacity-[0.5] texture-scanlines"
      />
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 z-40 crt-vignette"
      />
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 z-40 opacity-[0.1] mix-blend-screen texture-grain"
      />

      <LandingMasthead />

      <LandingHero onboarding={onboarding} />
      <MethodSection />
      <RecordSection />
      <SocialSection />
      <Colophon />
    </main>
  );
}
