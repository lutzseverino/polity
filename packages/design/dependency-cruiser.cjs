const base = require("../../.dependency-cruiser.base.cjs");

module.exports = {
  ...base,
  forbidden: [
    ...base.forbidden,
    {
      name: "design-is-app-agnostic",
      severity: "error",
      comment: "The design package must not depend on application code.",
      from: {},
      to: { path: "^../../apps/" },
    },
  ],
  options: {
    ...base.options,
    tsConfig: {
      fileName: "tsconfig.depcruise.json",
    },
  },
};
