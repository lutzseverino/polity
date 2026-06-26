const base = require("../../.dependency-cruiser.base.cjs");

module.exports = {
  ...base,
  forbidden: [
    ...base.forbidden,
    {
      name: "mobile-does-not-import-landing",
      severity: "error",
      comment:
        "Apps may share code through packages, not by importing from each other.",
      from: { path: "^src/" },
      to: { path: "^../landing/" },
    },
    {
      name: "mobile-uses-design-public-entrypoints",
      severity: "error",
      comment:
        "Mobile should import @polity/design entrypoints instead of package internals.",
      from: { path: "^src/" },
      to: {
        path: "^../../packages/design/(?:bin|src/(?!index[.]ts$|tokens[.]ts$|tokens[.]json$))",
      },
    },
    {
      name: "shared-components-do-not-reach-features",
      severity: "error",
      comment:
        "Reusable components should not depend on app routes or feature composition.",
      from: { path: "^src/components/" },
      to: { path: "^src/(?:app|features)/" },
    },
  ],
  options: {
    ...base.options,
    doNotFollow: {
      ...base.options.doNotFollow,
      path: `${base.options.doNotFollow.path}|^../../packages/design/`,
    },
    tsConfig: {
      fileName: "tsconfig.depcruise.json",
    },
  },
};
