const base = require("../../.dependency-cruiser.base.cjs");

module.exports = {
  ...base,
  forbidden: [
    ...base.forbidden,
    {
      name: "ui-does-not-import-apps",
      severity: "error",
      comment: "Shared UI cannot depend on application implementation.",
      from: {},
      to: { path: "^../../apps/" },
    },
    {
      name: "registry-source-stays-foundational",
      severity: "error",
      comment:
        "Registry-managed source may not depend on the public wrapper boundary.",
      from: { path: "^src/components/ui/" },
      to: { path: "^src/components/app/" },
    },
    {
      name: "raw-registry-source-is-private",
      severity: "error",
      comment:
        "Only package-owned public wrappers may import canonical registry source.",
      from: { path: "^src/(?!components/(?:app|ui)/)" },
      to: { path: "^src/components/ui/" },
    },
  ],
  options: {
    ...base.options,
    tsConfig: {
      fileName: "tsconfig.depcruise.json",
    },
  },
};
