import { defineConfig } from "@lingui/cli";

const routeCatalogs = [
  "actions",
  "explore",
  "home",
  "inbox",
  "me",
  "polities",
].map((route) => ({
  include: [`<rootDir>/src/routes/${route}/`],
  path: `<rootDir>/locales/{locale}/routes/${route}`,
}));

export default defineConfig({
  catalogs: [
    {
      include: ["<rootDir>/src/app/**"],
      path: "<rootDir>/locales/{locale}/app",
    },
    {
      include: ["<rootDir>/src/components/app/**"],
      path: "<rootDir>/locales/{locale}/components/app",
    },
    {
      include: ["<rootDir>/src/domains/{name}/"],
      path: "<rootDir>/locales/{locale}/domains/{name}",
    },
    {
      include: ["<rootDir>/src/features/{name}/"],
      path: "<rootDir>/locales/{locale}/features/{name}",
    },
    ...routeCatalogs,
    {
      include: [
        "<rootDir>/src/routes/__root.tsx",
        "<rootDir>/src/routes/index.tsx",
      ],
      path: "<rootDir>/locales/{locale}/routes/root",
    },
  ],
  catalogsMergePath: "<rootDir>/src/app/i18n/generated/{locale}/messages",
  fallbackLocales: { default: "en" },
  locales: ["en", "es", "pseudo"],
  pseudoLocale: {
    append: " ⟧",
    extend: 0.3,
    locale: "pseudo",
    prepend: "⟦ ",
  },
  sourceLocale: "en",
});
