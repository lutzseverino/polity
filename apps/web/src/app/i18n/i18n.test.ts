import { beforeEach, describe, expect, it } from "vitest";

import { activateLocale, i18n } from "@/app/i18n/i18n";
import { persistLocale, resolveInitialLocale } from "@/app/i18n/resolve-locale";

describe("locale runtime", () => {
  beforeEach(() => {
    window.localStorage.clear();
    activateLocale("en");
  });

  it("normalizes a persisted regional locale to its supported base locale", () => {
    window.localStorage.setItem("polity.locale", "es-MX");

    expect(resolveInitialLocale()).toBe("es");
  });

  it("persists an explicit locale choice", () => {
    persistLocale("pseudo");

    expect(resolveInitialLocale()).toBe("pseudo");
  });

  it("activates Lingui and updates document language metadata", () => {
    activateLocale("pseudo");

    expect(i18n.locale).toBe("pseudo");
    expect(document.documentElement.lang).toBe("en-XA");
    expect(document.documentElement.dir).toBe("ltr");
  });
});
