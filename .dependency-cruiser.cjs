const baseForbidden = [
  {
    name: "no-circular",
    severity: "error",
    comment:
      "Keep module graphs acyclic so ownership remains easy to reason about.",
    from: {},
    to: { circular: true },
  },
  {
    name: "not-to-unresolvable",
    severity: "error",
    comment: "Imports should resolve from the active workspace configuration.",
    from: {},
    to: { couldNotResolve: true },
  },
  {
    name: "landing-does-not-import-mobile",
    severity: "error",
    comment:
      "Apps may share code through packages, not by importing from each other.",
    from: { path: "^apps/landing/" },
    to: { path: "^apps/mobile/" },
  },
  {
    name: "mobile-does-not-import-landing",
    severity: "error",
    comment:
      "Apps may share code through packages, not by importing from each other.",
    from: { path: "^apps/mobile/" },
    to: { path: "^apps/landing/" },
  },
  {
    name: "design-is-app-agnostic",
    severity: "error",
    comment: "The design package must not depend on application code.",
    from: { path: "^packages/design/" },
    to: { path: "^apps/" },
  },
  {
    name: "apps-use-design-public-entrypoints",
    severity: "error",
    comment:
      "Apps should import @polity/design entrypoints instead of package internals.",
    from: { path: "^apps/" },
    to: {
      path: "^packages/design/(?:bin|src/(?!index[.]ts$|tokens[.]ts$|tokens[.]json$))",
    },
  },
  {
    name: "shared-components-do-not-reach-pages",
    severity: "error",
    comment:
      "Reusable components should not depend on page or feature composition.",
    from: { path: "^apps/[^/]+/src/components/" },
    to: { path: "^apps/[^/]+/src/(?:pages|features|app)/" },
  },
  {
    name: "ui-components-stay-foundational",
    severity: "error",
    comment:
      "Low-level UI components should stay below app-specific components.",
    from: { path: "^apps/[^/]+/src/components/ui/" },
    to: { path: "^apps/[^/]+/src/components/app/" },
  },
  {
    name: "generated-api-stays-behind-api-boundary",
    severity: "error",
    comment:
      "Generated API types should be wrapped by the local api boundary before app use.",
    from: { path: "^apps/landing/src/(?!api/)" },
    to: { path: "^apps/landing/src/api/generated/" },
  },
];

const baseOptions = {
  doNotFollow: {
    path: "node_modules",
    dependencyTypes: [
      "npm",
      "npm-dev",
      "npm-optional",
      "npm-peer",
      "npm-bundled",
      "npm-no-pkg",
    ],
  },
  combinedDependencies: true,
  enhancedResolveOptions: {
    exportsFields: ["exports"],
    conditionNames: ["import", "require", "node", "default"],
    mainFields: ["module", "main", "types", "typings"],
  },
};

module.exports = {
  forbidden: baseForbidden,
  options: baseOptions,
};
