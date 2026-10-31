#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import { createHash } from "node:crypto";
import { existsSync, readdirSync, readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

// Directories the pinned CLI writes into: the `ui` and `hooks` aliases resolve
// inside src/components/ui, and the `lib` and `utils` aliases inside src/lib.
const generatedRoots = ["src/components/ui", "src/lib"];
// Maintained guidance kept beside the generated components.
const maintainedFiles = new Set([
  "src/components/ui/AGENTS.md",
  "src/components/ui/README.md",
]);
const manifestName = "registry-manifest.json";
const schemaVersion = 2;
// shadcn add options that take a value, which is not a component name.
const optionsWithValues = new Set(["-c", "--cwd", "-p", "--path"]);

function readJson(filePath) {
  return JSON.parse(readFileSync(filePath, "utf8"));
}

function filesUnder(packageRoot, directory) {
  const absolute = path.join(packageRoot, directory);
  if (!existsSync(absolute)) {
    return [];
  }
  return readdirSync(absolute, { withFileTypes: true }).flatMap((entry) => {
    const relative = `${directory}/${entry.name}`;
    return entry.isDirectory() ? filesUnder(packageRoot, relative) : relative;
  });
}

export function generatedFiles(packageRoot) {
  return generatedRoots
    .flatMap((root) => filesUnder(packageRoot, root))
    .filter((file) => !maintainedFiles.has(file))
    .sort();
}

function sha256(content) {
  return createHash("sha256").update(content).digest("hex");
}

// The pinned CLI and style that produce the generated output.
function currentProvenance(packageRoot) {
  const packageJson = readJson(path.join(packageRoot, "package.json"));
  const componentsJson = readJson(path.join(packageRoot, "components.json"));
  return {
    generator: {
      package: "shadcn",
      version: packageJson.devDependencies?.shadcn,
    },
    style: componentsJson.style,
  };
}

export function writeManifest(packageRoot, components) {
  const manifest = {
    schemaVersion,
    ...currentProvenance(packageRoot),
    components: [...new Set(components)].sort(),
    files: Object.fromEntries(
      generatedFiles(packageRoot).map((file) => [
        file,
        sha256(readFileSync(path.join(packageRoot, file))),
      ]),
    ),
  };
  writeFileSync(
    path.join(packageRoot, manifestName),
    `${JSON.stringify(manifest, null, 2)}\n`,
  );
}

export function readManifest(packageRoot) {
  return readJson(path.join(packageRoot, manifestName));
}

// Compares the committed bytes with the manifest without contacting the
// registry. Returns one message per difference.
export function checkRegistry(packageRoot) {
  const manifest = readManifest(packageRoot);
  const expected = currentProvenance(packageRoot);
  const problems = [];

  if (manifest.schemaVersion !== schemaVersion) {
    problems.push(`${manifestName} must use schemaVersion ${schemaVersion}.`);
  }
  if (manifest.generator?.package !== expected.generator.package) {
    problems.push(`${manifestName} must record the shadcn generator.`);
  }
  if (manifest.generator?.version !== expected.generator.version) {
    problems.push(
      `${manifestName} records shadcn ${manifest.generator?.version}, but package.json pins ${expected.generator.version}.`,
    );
  }
  if (manifest.style !== expected.style) {
    problems.push(
      `${manifestName} records style ${manifest.style}, but components.json uses ${expected.style}.`,
    );
  }
  if (
    !Array.isArray(manifest.components) ||
    manifest.components.length === 0 ||
    manifest.components.some((name) => typeof name !== "string" || !name)
  ) {
    problems.push(
      `${manifestName} must list the recorded registry components to refresh.`,
    );
  }

  const recorded = manifest.files ?? {};
  const present = new Set(generatedFiles(packageRoot));
  for (const [file, digest] of Object.entries(recorded)) {
    if (!present.has(file)) {
      problems.push(`Missing generated file: ${file}`);
    } else if (sha256(readFileSync(path.join(packageRoot, file))) !== digest) {
      problems.push(`Changed generated file: ${file}`);
    }
  }
  for (const file of present) {
    if (!Object.hasOwn(recorded, file)) {
      problems.push(`Unexpected generated file: ${file}`);
    }
  }
  return problems;
}

function runShadcn(packageRoot, args) {
  const result = spawnSync("pnpm", ["exec", "shadcn", ...args], {
    cwd: packageRoot,
    stdio: "inherit",
  });

  if (result.status !== 0) {
    process.exit(result.status ?? 1);
  }
}

function componentNames(args) {
  return args.filter(
    (arg, index) =>
      !arg.startsWith("-") && !optionsWithValues.has(args[index - 1]),
  );
}

function main(packageRoot, [command, ...args]) {
  switch (command) {
    case "add": {
      const names = componentNames(args);
      if (names.length === 0) {
        console.error("Usage: pnpm --filter @polity/ui ui:add <component...>");
        process.exit(1);
      }
      const recorded = existsSync(path.join(packageRoot, manifestName))
        ? (readManifest(packageRoot).components ?? [])
        : [];
      runShadcn(packageRoot, ["add", ...args]);
      writeManifest(packageRoot, [...recorded, ...names]);
      break;
    }
    case "check": {
      const problems = checkRegistry(packageRoot);
      if (problems.length > 0) {
        console.error(
          [
            `Generated registry output differs from ${manifestName}. Do not edit src/components/ui or src/lib by hand; run ui:add or ui:update.`,
            ...problems.map((problem) => `- ${problem}`),
          ].join("\n"),
        );
        process.exit(1);
      }
      break;
    }
    case "update": {
      const { components, files } = readManifest(packageRoot);
      runShadcn(packageRoot, ["add", ...components, "--overwrite", "--yes"]);
      // A refresh may only rewrite recorded files; anything new came from a
      // registry change and needs ui:add so it is chosen deliberately.
      const added = generatedFiles(packageRoot).filter(
        (file) => !Object.hasOwn(files, file),
      );
      if (added.length > 0) {
        console.error(
          [
            `The refresh created files ${manifestName} does not record. Remove them, or add their components with ui:add:`,
            ...added.map((file) => `- ${file}`),
          ].join("\n"),
        );
        process.exit(1);
      }
      writeManifest(packageRoot, components);
      break;
    }
    default:
      console.error(
        "Usage: registry.mjs <add <component...> | check | update>",
      );
      process.exit(1);
  }
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  main(
    path.resolve(path.dirname(fileURLToPath(import.meta.url)), ".."),
    process.argv.slice(2),
  );
}
