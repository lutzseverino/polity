import { Code2 } from "lucide-react";
import { useTranslation } from "react-i18next";

import { LanguagePicker } from "@/components/app/language-picker";
import { ThemeSwitcher } from "@/components/app/theme-switcher";
import { Button } from "@/components/ui/button";

export function LandingNavbar() {
  const { t } = useTranslation("common");

  return (
    <header className="sticky top-0 z-40 border-b bg-background/85 backdrop-blur-sm">
      <div aria-hidden="true" className="h-1 bg-primary" />
      <div className="mx-auto flex h-14 max-w-7xl items-center justify-between gap-4 px-4 md:px-8">
        <a
          aria-label={t("brand.homeAria")}
          className="flex items-center gap-3"
          href="/"
        >
          <span className="font-display text-2xl leading-none tracking-tight">
            decreos
          </span>
          <span className="hidden font-mono text-[0.65rem] leading-none tracking-[0.24em] text-muted-foreground uppercase sm:inline">
            {t("brand.tagline")}
          </span>
        </a>
        <div className="flex items-center gap-3 md:gap-5">
          <div className="flex items-center gap-2">
            <LanguagePicker />
            <ThemeSwitcher />
          </div>
          <span className="hidden font-mono text-[0.65rem] leading-none tracking-[0.22em] text-muted-foreground uppercase md:inline">
            {t("brand.established")}
          </span>
          <Button
            aria-label={t("brand.githubAria")}
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
  );
}
