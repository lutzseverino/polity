import i18next from "i18next";
import { initReactI18next } from "react-i18next";

import commonEn from "./locales/en/common.json";
import landingEn from "./locales/en/landing.json";
import commonEs from "./locales/es/common.json";
import landingEs from "./locales/es/landing.json";

export const languages = ["en", "es"] as const;
export type Language = (typeof languages)[number];

export const languageNames = {
  en: "English",
  es: "Español",
} satisfies Record<Language, string>;

const languageStorageKey = "polity-landing-language";

function isLanguage(value: string | null | undefined): value is Language {
  return languages.some((language) => language === value);
}

function getInitialLanguage() {
  if (typeof window === "undefined") {
    return "en";
  }

  const storedLanguage = window.localStorage.getItem(languageStorageKey);
  if (isLanguage(storedLanguage)) {
    return storedLanguage;
  }

  const browserLanguage = window.navigator.language.split("-")[0];
  return isLanguage(browserLanguage) ? browserLanguage : "en";
}

i18next.use(initReactI18next).init({
  fallbackLng: "en",
  interpolation: {
    escapeValue: false,
  },
  lng: getInitialLanguage(),
  ns: ["common", "landing"],
  defaultNS: "landing",
  resources: {
    en: {
      common: commonEn,
      landing: landingEn,
    },
    es: {
      common: commonEs,
      landing: landingEs,
    },
  },
  supportedLngs: languages,
});

i18next.on("languageChanged", (language) => {
  if (!isLanguage(language) || typeof document === "undefined") {
    return;
  }

  document.documentElement.lang = language;
  window.localStorage.setItem(languageStorageKey, language);
});

if (typeof document !== "undefined") {
  document.documentElement.lang = i18next.language;
}

export { i18next };
