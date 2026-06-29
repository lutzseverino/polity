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
      {/* Tube-monitor atmosphere: scanlines, an edge vignette, and paper grain,
          all click-through and held above the content. */}
      <div
        aria-hidden="true"
        className="pointer-events-none fixed inset-0 z-40 opacity-[0.5] texture-scanlines"
      />
      <div
        aria-hidden="true"
        className="pointer-events-none fixed inset-0 z-40 crt-vignette"
      />
      <div
        aria-hidden="true"
        className="pointer-events-none fixed inset-0 z-40 opacity-[0.04] mix-blend-screen texture-grain"
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
