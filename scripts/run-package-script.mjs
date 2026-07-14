#!/usr/bin/env node
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const SUPPORTED_MANAGERS = new Set(["npm", "pnpm"]);
const workspaceRoot = fileURLToPath(new URL("..", import.meta.url));

function detectPackageManager() {
  const configuredManager = process.env.POLITY_PACKAGE_MANAGER;

  if (configuredManager) {
    return configuredManager;
  }

  const userAgent = process.env.npm_config_user_agent ?? "";

  if (userAgent.startsWith("npm/")) {
    return "npm";
  }

  if (userAgent.startsWith("pnpm/")) {
    return "pnpm";
  }

  return "pnpm";
}

function printUsageAndExit() {
  console.error(
    "Usage: run-package-script.mjs root <script> [args...] | workspace <workspace-path> <script> [args...]",
  );
  process.exit(1);
}

const [scope, target, script, ...scriptArgs] = process.argv.slice(2);

if (!scope || !target || (scope === "workspace" && !script)) {
  printUsageAndExit();
}

const packageManager = detectPackageManager();

if (!SUPPORTED_MANAGERS.has(packageManager)) {
  console.error(
    `Unsupported package manager "${packageManager}". Use npm or pnpm, or set POLITY_PACKAGE_MANAGER.`,
  );
  process.exit(1);
}

let command;
let args;

if (scope === "root") {
  command = packageManager;
  args = [
    "run",
    target,
    ...(scriptArgs.length > 0 ? ["--", ...scriptArgs] : []),
  ];
} else if (scope === "workspace") {
  command = packageManager;
  args =
    packageManager === "pnpm"
      ? [
          "--dir",
          target,
          "run",
          script,
          ...(scriptArgs.length > 0 ? ["--", ...scriptArgs] : []),
        ]
      : [
          "--workspace",
          target,
          "run",
          script,
          ...(scriptArgs.length > 0 ? ["--", ...scriptArgs] : []),
        ];
} else {
  printUsageAndExit();
}

const result = spawnSync(command, args, {
  cwd: workspaceRoot,
  shell: process.platform === "win32",
  stdio: "inherit",
});

if (result.error) {
  console.error(result.error.message);
  process.exit(1);
}

process.exit(result.status ?? 1);
