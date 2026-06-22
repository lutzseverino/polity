#!/usr/bin/env node
import { spawnSync } from "node:child_process";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");

const targets = {
  landing: {
    cwd: root,
    config: resolve(root, ".dependency-cruiser.landing.cjs"),
    paths: ["apps/landing/src", "packages/design/src", "packages/design/bin"],
  },
  mobile: {
    cwd: root,
    config: resolve(root, ".dependency-cruiser.mobile.cjs"),
    paths: ["apps/mobile/src", "packages/design/src", "packages/design/bin"],
  },
  design: {
    cwd: root,
    config: resolve(root, ".dependency-cruiser.design.cjs"),
    paths: ["packages/design/src", "packages/design/bin"],
  },
};

const selectedTarget = process.argv[2] ?? "all";
const selectedTargets =
  selectedTarget === "all"
    ? Object.entries(targets)
    : [[selectedTarget, targets[selectedTarget]]];

if (selectedTargets.some(([, target]) => !target)) {
  console.error(
    `Unknown target "${selectedTarget}". Use one of: all, ${Object.keys(targets).join(", ")}.`,
  );
  process.exit(1);
}

for (const [name, target] of selectedTargets) {
  console.log(`\nChecking ${name} TypeScript architecture...`);

  const result = spawnSync(
    "pnpm",
    ["exec", "depcruise", "--config", target.config, ...target.paths],
    {
      cwd: target.cwd,
      shell: process.platform === "win32",
      stdio: "inherit",
    },
  );

  if (result.error) {
    console.error(result.error.message);
    process.exit(1);
  }

  if (result.status !== 0) {
    process.exit(result.status ?? 1);
  }
}
