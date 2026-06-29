import { useTranslation } from "react-i18next";

import { LanguagePicker } from "@/components/app/language-picker";
import { ThemeSwitcher } from "@/components/app/theme-switcher";

/**
 * Masthead — the terminal's title bar. A single full-width rule with the
 * wordmark on the left and the utility controls on the right, so the page opens
 * like a console session rather than app chrome. The bar is translucent and
 * blurred so the ledger and seal stay legible scrolling underneath.
 */
export function LandingMasthead() {
  const { t } = useTranslation("common");

  return (
    <header className="fixed inset-x-0 top-0 z-50 border-b bg-background/80 backdrop-blur-sm">
      <div className="mx-auto flex h-12 max-w-7xl items-center justify-between gap-4 px-4 md:px-8">
        <a
          aria-label={t("brand.homeAria")}
          className="brand-trigger inline-flex items-center gap-3"
          href="/"
        >
          <span
            aria-hidden="true"
            className="brand-mark block size-2.5 bg-primary"
          />
          <span className="font-display text-lg leading-none">decreos</span>
        </a>

        <div className="flex items-center gap-3 md:gap-4">
          <LanguagePicker />
          <ThemeSwitcher />
        </div>
      </div>
    </header>
  );
}
