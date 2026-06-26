import { useTranslation } from "react-i18next";

import { LanguagePicker } from "@/components/app/language-picker";
import { ThemeSwitcher } from "@/components/app/theme-switcher";

/**
 * Corner masthead — the navbar-free brand lockup. The wordmark and utility
 * controls pin to opposite top corners with no bar between them, so the page
 * reads as a poster rather than an app chrome. The wrapper is click-through;
 * only the wordmark and controls take pointer events. Both corners sit on a
 * translucent, blurred plate so they stay legible over the light hero and the
 * dark record / founding grounds alike, regardless of what scrolls behind.
 */
export function LandingMasthead() {
  const { t } = useTranslation("common");

  return (
    <div className="pointer-events-none fixed inset-x-0 top-0 z-50">
      <div className="mx-auto flex max-w-7xl items-start justify-between gap-4 px-4 py-4 md:px-8 md:py-6">
        <a
          aria-label={t("brand.homeAria")}
          className="pointer-events-auto inline-flex items-center gap-2.5 border bg-background/75 px-3 py-2 backdrop-blur-sm"
          href="/"
        >
          <span aria-hidden="true" className="block size-2 bg-primary" />
          <span className="font-display text-xl leading-none tracking-tight">
            decreos
          </span>
        </a>
        <div className="pointer-events-auto flex items-center gap-2">
          <LanguagePicker />
          <ThemeSwitcher />
        </div>
      </div>
    </div>
  );
}
