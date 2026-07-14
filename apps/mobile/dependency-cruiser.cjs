const base = require("../../.dependency-cruiser.base.cjs");

module.exports = {
  ...base,
  forbidden: [
    ...base.forbidden,
    {
      name: "mobile-does-not-import-other-apps",
      severity: "error",
      comment:
        "Applications must not import implementation from sibling applications.",
      from: { path: "^src/" },
      to: { path: "^../(?:landing|web)/" },
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
    tsConfig: {
      fileName: "tsconfig.depcruise.json",
    },
  },
};
