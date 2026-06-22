const base = require("./.dependency-cruiser.cjs");

module.exports = {
  ...base,
  options: {
    ...base.options,
    tsConfig: {
      fileName: "tsconfig.depcruise.design.json",
    },
  },
};
