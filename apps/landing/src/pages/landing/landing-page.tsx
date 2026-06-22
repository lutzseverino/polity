import { useRef } from "react";
import { useLandingMotion } from "./motion";
import { LandingNavbar } from "./navbar";
import { useLandingOnboarding } from "./onboarding";
import {
  Colophon,
  LandingHero,
  MethodSection,
  RecordSection,
} from "./sections";

export function LandingPage() {
  const onboarding = useLandingOnboarding();
  const scope = useRef<HTMLElement>(null);

  useLandingMotion(scope);

  return (
    <main
      className="relative min-h-[100dvh] bg-background text-foreground [overflow-anchor:none]"
      data-landing-page
      ref={scope}
    >
      <div
        aria-hidden="true"
        className="pointer-events-none fixed inset-0 z-50 opacity-[0.04] mix-blend-multiply texture-grain dark:opacity-[0.05] dark:mix-blend-screen"
      />

      <LandingNavbar />

      <LandingHero onboarding={onboarding} />
      <MethodSection />
      <RecordSection />
      <Colophon />
    </main>
  );
}
