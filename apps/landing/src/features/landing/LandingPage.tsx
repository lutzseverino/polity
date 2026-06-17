import { Code2 } from "lucide-react";
import { useRef } from "react";

import { Button } from "@/components/ui/button";

import { useLandingMotion } from "./LandingPage.animations";
import { useLandingOnboarding } from "./LandingPage.hooks";
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

      <header className="sticky top-0 z-40 border-b bg-background/85 backdrop-blur-sm">
        <div aria-hidden="true" className="h-1 bg-primary" />
        <div className="mx-auto flex h-14 max-w-7xl items-center justify-between gap-4 px-4 md:px-8">
          <a
            aria-label="decreos home"
            className="flex items-center gap-3"
            href="/"
          >
            <span className="font-display text-2xl leading-none tracking-tight">
              decreos
            </span>
            <span className="hidden font-mono text-[0.65rem] leading-none tracking-[0.24em] text-muted-foreground uppercase sm:inline">
              Self-government, founded
            </span>
          </a>
          <div className="flex items-center gap-5">
            <span className="hidden font-mono text-[0.65rem] leading-none tracking-[0.22em] text-muted-foreground uppercase md:inline">
              Est. MMXXVI
            </span>
            <Button
              aria-label="Open decreos on GitHub"
              asChild
              size="icon"
              variant="ghost"
            >
              <a
                href="https://github.com/lutzseverino/polity"
                rel="noreferrer"
                target="_blank"
              >
                <Code2 aria-hidden="true" />
              </a>
            </Button>
          </div>
        </div>
      </header>

      <LandingHero onboarding={onboarding} />
      <MethodSection />
      <RecordSection />
      <Colophon />
    </main>
  );
}

export type LandingOnboarding = ReturnType<typeof useLandingOnboarding>;
