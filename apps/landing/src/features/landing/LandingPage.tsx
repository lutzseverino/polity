import { useRef } from "react";
import { useLandingMotion } from "./LandingPage.animations";
import { useLandingOnboarding } from "./LandingPage.hooks";
import { LandingNavbar } from "./LandingPage.navbar";
import {
  Colophon,
  LandingHero,
  MethodSection,
  RecordSection,
} from "./LandingPage.sections";

export function LandingPage() {
  const onboarding = useLandingOnboarding();
  const scope = useRef<HTMLElement>(null);

  useLandingMotion(scope);

  return (
    <main
      className="relative min-h-[100dvh] bg-background text-foreground"
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

export type LandingOnboarding = ReturnType<typeof useLandingOnboarding>;
