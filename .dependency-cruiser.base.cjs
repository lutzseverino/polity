const forbidden = [
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
];

const options = {
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
  forbidden,
  options,
};
