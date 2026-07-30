const base = require("../../.dependency-cruiser.base.cjs");

module.exports = {
  ...base,
  forbidden: [
    ...base.forbidden,
    {
      name: "landing-does-not-import-other-apps",
      severity: "error",
      comment:
        "Apps may share code through packages, not by importing from each other.",
      from: { path: "^src/" },
      to: { path: "^../(?:mobile|web)/" },
    },
    {
      name: "landing-uses-design-public-entrypoints",
      severity: "error",
      comment:
        "Landing should import @polity/design entrypoints instead of package internals.",
      from: { path: "^src/" },
      to: {
        path: "^../../packages/design/(?:bin|src/(?!index[.]ts$|tokens[.]ts$|tokens[.]json$))",
      },
    },
    {
      name: "shared-components-do-not-reach-pages",
      severity: "error",
      comment:
        "Reusable components should not depend on page or feature composition.",
      from: { path: "^src/components/" },
      to: { path: "^src/(?:pages|features|app)/" },
    },
    {
      name: "generated-api-stays-behind-api-boundary",
      severity: "error",
      comment:
        "Generated API types should be wrapped by the local api boundary before app use.",
      from: { path: "^src/(?!api/)" },
      to: { path: "^src/api/generated/" },
    },
  ],
  options: {
    ...base.options,
    doNotFollow: {
      ...base.options.doNotFollow,
      path: `${base.options.doNotFollow.path}|^../../packages/design/|^../../packages/ui/`,
    },
    tsConfig: {
      fileName: "tsconfig.depcruise.json",
    },
  },
};
