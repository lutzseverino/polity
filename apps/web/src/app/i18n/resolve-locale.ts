import {
  defaultLocale,
  isSupportedLocale,
  type SupportedLocale,
} from "@/app/i18n/supported-locales";

const localeStorageKey = "polity.locale";

function normalizeLocale(locale: string | null | undefined) {
  if (!locale) {
    return undefined;
  }

  if (isSupportedLocale(locale)) {
    return locale;
  }

  const baseLocale = locale.split("-")[0];
  return baseLocale && isSupportedLocale(baseLocale) ? baseLocale : undefined;
}

export function resolveInitialLocale(): SupportedLocale {
  if (typeof window === "undefined") {
    return defaultLocale;
  }

  const persistedLocale = normalizeLocale(
    window.localStorage.getItem(localeStorageKey),
  );

  if (persistedLocale) {
    return persistedLocale;
  }

  for (const locale of window.navigator.languages) {
    const supportedLocale = normalizeLocale(locale);

    if (supportedLocale && supportedLocale !== "pseudo") {
      return supportedLocale;
    }
  }

  return defaultLocale;
}

export function persistLocale(locale: SupportedLocale) {
  window.localStorage.setItem(localeStorageKey, locale);
}
