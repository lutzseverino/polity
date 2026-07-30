#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import { createHash } from "node:crypto";
import { readdirSync, readFileSync, statSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const packageRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
);
const registryRoot = path.join(packageRoot, "src/components/ui");
const manifestPath = path.join(packageRoot, "registry-manifest.json");
const packageJson = JSON.parse(
  readFileSync(path.join(packageRoot, "package.json"), "utf8"),
);
const componentsJson = JSON.parse(
  readFileSync(path.join(packageRoot, "components.json"), "utf8"),
);

function sourceFiles(directory) {
  return readdirSync(directory)
    .flatMap((entry) => {
      const filePath = path.join(directory, entry);
      return statSync(filePath).isDirectory()
        ? sourceFiles(filePath)
        : filePath;
    })
    .filter((filePath) => /[.](?:ts|tsx)$/.test(filePath))
    .sort();
}

function sha256(content) {
  return createHash("sha256").update(content).digest("hex");
}

function currentManifest() {
  return {
    schemaVersion: 1,
    generator: {
      package: "shadcn",
      version: packageJson.devDependencies.shadcn,
    },
    style: componentsJson.style,
    files: Object.fromEntries(
      sourceFiles(registryRoot).map((filePath) => [
        path.relative(packageRoot, filePath),
        sha256(readFileSync(filePath)),
      ]),
    ),
  };
}

function writeManifest() {
  writeFileSync(
    manifestPath,
    `${JSON.stringify(currentManifest(), null, 2)}\n`,
  );
}

function checkManifest() {
  const expected = JSON.parse(readFileSync(manifestPath, "utf8"));
  const current = currentManifest();

  if (JSON.stringify(expected) !== JSON.stringify(current)) {
    console.error(
      "Canonical registry provenance is stale. Do not edit src/components/ui manually; run pnpm ui:add or pnpm ui:update.",
    );
    process.exit(1);
  }
}

function relativeImport(fromFile, toFile) {
  const relative = path
    .relative(path.dirname(fromFile), toFile)
    .replaceAll(path.sep, "/")
    .replace(/[.](?:ts|tsx)$/, "");
  return relative.startsWith(".") ? relative : `./${relative}`;
}

function normalizeRegistryImports() {
  const utilsPath = path.join(packageRoot, "src/lib/utils.ts");

  for (const filePath of sourceFiles(registryRoot)) {
    const source = readFileSync(filePath, "utf8");
    const normalized = source
      .replaceAll("@/lib/utils", relativeImport(filePath, utilsPath))
      .replace(/@\/components\/ui\/([A-Za-z0-9_./-]+)/g, (_match, target) =>
        relativeImport(filePath, path.join(registryRoot, `${target}.tsx`)),
      );

    if (normalized !== source) {
      writeFileSync(filePath, normalized);
    }
  }
}

function runShadcn(args) {
  const result = spawnSync("pnpm", ["exec", "shadcn", ...args], {
    cwd: packageRoot,
    stdio: "inherit",
  });

  if (result.status !== 0) {
    process.exit(result.status ?? 1);
  }

  normalizeRegistryImports();
  writeManifest();
}

const [command, ...args] = process.argv.slice(2);

switch (command) {
  case "add":
    if (args.length === 0) {
      console.error("Usage: pnpm --filter @polity/ui ui:add <component...>");
      process.exit(1);
    }
    runShadcn(["add", ...args]);
    break;
  case "check":
    checkManifest();
    break;
  case "update":
    runShadcn(["add", "--all", "--overwrite"]);
    break;
  default:
    console.error("Usage: registry.mjs <add <component...> | check | update>");
    process.exit(1);
}
