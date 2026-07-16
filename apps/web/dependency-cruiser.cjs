const base = require("../../.dependency-cruiser.base.cjs");

module.exports = {
  ...base,
  forbidden: [
    ...base.forbidden,
    {
      name: "web-does-not-import-other-apps",
      severity: "error",
      comment:
        "Applications must not import implementation from sibling applications.",
      from: { path: "^src/" },
      to: { path: "^../(?:landing|mobile)/" },
    },
    {
      name: "api-stays-foundational",
      severity: "error",
      comment:
        "Shared transport mechanics cannot depend on application, product, or presentation owners.",
      from: { path: "^src/api/" },
      to: { path: "^src/(?:app|components|domains|features|routes)/" },
    },
    {
      name: "axios-is-owned-by-api",
      severity: "error",
      comment:
        "Only the shared transport boundary may depend directly on Axios.",
      from: { path: "^src/(?!api/)" },
      to: { path: "node_modules/axios/" },
    },
    {
      name: "react-query-stays-in-data-boundaries",
      severity: "error",
      comment:
        "Application composition and owner-local API modules own TanStack Query integration.",
      from: {
        path: "^src/(?!app/|domains/[^/]+/api/|features/[^/]+/api/)",
      },
      to: { path: "node_modules/@tanstack/react-query/" },
    },
    {
      name: "ui-components-stay-foundational",
      severity: "error",
      comment:
        "Registry-managed shadcn components may only compose other registry components and low-level helpers.",
      from: { path: "^src/components/ui/" },
      to: { path: "^src/(?:app|components/app|domains|features|routes)/" },
    },
    {
      name: "application-code-uses-app-component-wrappers",
      severity: "error",
      comment:
        "Only app-owned component wrappers may import registry-managed shadcn components.",
      from: { path: "^src/(?!components/(?:app|ui)/)" },
      to: { path: "^src/components/ui/" },
    },
    {
      name: "app-components-stay-owner-neutral",
      severity: "error",
      comment:
        "Reusable app components must not depend on application composition, domains, features, or routes.",
      from: { path: "^src/components/app/" },
      to: { path: "^src/(?:app|domains|features|routes)/" },
    },
    {
      name: "domains-do-not-reach-upward",
      severity: "error",
      comment:
        "Domains own reusable nouns and cannot depend on features, routes, or app composition.",
      from: { path: "^src/domains/" },
      to: { path: "^src/(?:app|features|routes)/" },
    },
    {
      name: "domains-do-not-own-routing",
      severity: "error",
      comment:
        "Domain components stay presentational; routes and features own URL navigation. InboxItemLink remains an explicit migration exception.",
      from: {
        path: "^src/domains/",
        pathNot:
          "^src/domains/inbox/components/InboxItemLink/InboxItemLink[.]tsx$",
      },
      to: { path: "node_modules/@tanstack/react-router/" },
    },
    {
      name: "features-do-not-reach-upward",
      severity: "error",
      comment:
        "Features own user actions and cannot depend on app composition or routes.",
      from: { path: "^src/features/" },
      to: { path: "^src/(?:app|routes)/" },
    },
    {
      name: "routes-do-not-import-app-composition",
      severity: "error",
      comment:
        "Route modules compose lower layers. Only the root route may install the app shell and route state UI.",
      from: { path: "^src/routes/(?!__root[.]tsx$)" },
      to: { path: "^src/app/" },
    },
    {
      name: "external-code-uses-domain-public-entrypoints",
      severity: "error",
      comment:
        "Code outside a domain must enter it through the domain index so internals remain movable.",
      from: { path: "^src/(?!domains/)" },
      to: { path: "^src/domains/[^/]+/(?!index[.]ts$)" },
    },
    {
      name: "cross-domain-consumers-use-public-entrypoints",
      severity: "error",
      comment:
        "A domain may use another domain only through that domain's public entrypoint.",
      from: { path: "^src/domains/([^/]+)/" },
      to: {
        path: "^src/domains/[^/]+/(?!index[.]ts$)",
        pathNot: "^src/domains/$1/",
      },
    },
    {
      name: "external-code-uses-feature-public-entrypoints",
      severity: "error",
      comment:
        "Code outside a feature must enter it through the feature index so internals remain movable.",
      from: { path: "^src/(?!features/)" },
      to: { path: "^src/features/[^/]+/(?!index[.]ts$)" },
    },
    {
      name: "cross-feature-consumers-use-public-entrypoints",
      severity: "error",
      comment:
        "A feature may use another feature only through that feature's public entrypoint.",
      from: { path: "^src/features/([^/]+)/" },
      to: {
        path: "^src/features/[^/]+/(?!index[.]ts$)",
        pathNot: "^src/features/$1/",
      },
    },
    {
      name: "external-code-uses-app-component-entrypoints",
      severity: "error",
      comment:
        "Code outside an app component must enter it through the component index.",
      from: { path: "^src/(?!components/app/)" },
      to: { path: "^src/components/app/[^/]+/(?!index[.]ts$)" },
    },
    {
      name: "cross-component-consumers-use-public-entrypoints",
      severity: "error",
      comment:
        "An app component may use another app component only through that component's index.",
      from: { path: "^src/components/app/([^/]+)/" },
      to: {
        path: "^src/components/app/[^/]+/(?!index[.]ts$)",
        pathNot: "^src/components/app/$1/",
      },
    },
    {
      name: "owner-internals-do-not-import-own-entrypoint",
      severity: "error",
      comment:
        "Domain and feature internals import sibling implementation modules directly to avoid self-barrel cycles.",
      from: {
        path: "^src/(domains|features)/([^/]+)/(?!index[.]ts$)(?!.*[.](?:test|spec)[.])",
      },
      to: { path: "^src/$1/$2/index[.]ts$" },
    },
    {
      name: "component-internals-do-not-import-own-entrypoint",
      severity: "error",
      comment:
        "App component internals import sibling implementation modules directly to avoid self-barrel cycles.",
      from: {
        path: "^src/components/app/([^/]+)/(?!index[.]ts$)(?!.*[.](?:test|spec)[.])",
      },
      to: { path: "^src/components/app/$1/index[.]ts$" },
    },
  ],
  options: {
    ...base.options,
    tsConfig: {
      fileName: "tsconfig.depcruise.json",
    },
  },
};
