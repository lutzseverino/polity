import { i18n } from "@lingui/core";

import { messages as enMessages } from "@/app/i18n/generated/en/messages";
import { messages as esMessages } from "@/app/i18n/generated/es/messages";
import { messages as pseudoMessages } from "@/app/i18n/generated/pseudo/messages";
import { persistLocale, resolveInitialLocale } from "@/app/i18n/resolve-locale";
import type { SupportedLocale } from "@/app/i18n/supported-locales";

i18n.load({
  en: enMessages,
  es: esMessages,
  pseudo: pseudoMessages,
});

export function activateLocale(
  locale: SupportedLocale,
  options: Readonly<{ persist?: boolean }> = {},
) {
  i18n.activate(locale);

  if (typeof document !== "undefined") {
    document.documentElement.lang = locale === "pseudo" ? "en-XA" : locale;
    document.documentElement.dir = "ltr";
  }

  if (options.persist && typeof window !== "undefined") {
    persistLocale(locale);
  }
}

activateLocale(resolveInitialLocale());

export { i18n };
