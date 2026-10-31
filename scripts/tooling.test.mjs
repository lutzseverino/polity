import assert from "node:assert/strict";
import { execFileSync, spawnSync } from "node:child_process";
import {
  copyFileSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import test from "node:test";

import prettier from "prettier";

const repositoryRoot = path.resolve(import.meta.dirname, "..");
const oxlint = path.join(repositoryRoot, "node_modules/.bin/oxlint");
const tsgolint = path.join(repositoryRoot, "node_modules/.bin/tsgolint");

// Lints probe files laid out under a copy of the root configuration, so the
// configuration's path-scoped rules apply without touching the source tree.
function lintProbes(probes, { typeAware = false } = {}) {
  const directory = mkdtempSync(path.join(tmpdir(), "polity-oxlint-"));
  try {
    copyFileSync(
      path.join(repositoryRoot, ".oxlintrc.json"),
      path.join(directory, ".oxlintrc.json"),
    );
    for (const [file, source] of Object.entries(probes)) {
      mkdirSync(path.dirname(path.join(directory, file)), { recursive: true });
      writeFileSync(path.join(directory, file), source);
    }
    const result = spawnSync(
      oxlint,
      [...(typeAware ? ["--type-aware"] : []), "--format", "json", "."],
      {
        cwd: directory,
        encoding: "utf8",
        env: { ...process.env, OXLINT_TSGOLINT_PATH: tsgolint },
      },
    );
    const diagnostics = JSON.parse(result.stdout).diagnostics;
    return Object.fromEntries(
      Object.keys(probes).map((file) => [
        file,
        [
          ...new Set(
            diagnostics
              .filter((diagnostic) => diagnostic.filename === file)
              .map((diagnostic) => diagnostic.code),
          ),
        ].sort(),
      ]),
    );
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
}

const directFetchSource = 'void self.fetch("/api/example");\nexport {};\n';

test("rejects direct fetch in production web source", () => {
  const results = lintProbes({
    "apps/web/src/domains/membership/api/membership-requests.ts":
      directFetchSource,
    "apps/web/src/features/sign-in/api/sign-in.ts":
      'void fetch("/api/example");\nexport {};\n',
  });

  assert.deepEqual(results, {
    "apps/web/src/domains/membership/api/membership-requests.ts": [
      "eslint(no-restricted-properties)",
    ],
    "apps/web/src/features/sign-in/api/sign-in.ts": [
      "eslint(no-restricted-globals)",
    ],
  });
});

test("allows direct fetch in the shared boundary, tests, and mocks", () => {
  const results = lintProbes({
    "apps/web/src/api/http-client.ts": directFetchSource,
    "apps/web/src/api/http-client.test.ts": directFetchSource,
    "apps/web/src/mocks/development-api-source.ts": directFetchSource,
  });

  for (const codes of Object.values(results)) {
    assert.deepEqual(codes, []);
  }
});

const sharedUiSource =
  'import { Button } from "@polity/ui/button";\nexport { Button };\n';

test("rejects shared UI imports outside app-owned wrappers", () => {
  const results = lintProbes({
    "apps/web/src/domains/membership/index.ts": sharedUiSource,
    "apps/landing/src/pages/landing/sections.ts": sharedUiSource,
  });

  for (const codes of Object.values(results)) {
    assert.deepEqual(codes, ["eslint(no-restricted-imports)"]);
  }
});

test("allows app-owned wrappers to consume shared UI", () => {
  const results = lintProbes({
    "apps/web/src/components/app/AppButton/AppButton.tsx": sharedUiSource,
    "apps/landing/src/components/app/app-button.tsx": sharedUiSource,
  });

  for (const codes of Object.values(results)) {
    assert.deepEqual(codes, []);
  }
});

test("enforces hooks rules in application source", () => {
  const results = lintProbes({
    "apps/web/src/features/sign-in/components/Probe.tsx": [
      'import { useState } from "react";',
      "export function Probe({ on }: { on: boolean }) {",
      "  if (on) {",
      "    useState(0);",
      "  }",
      "  return null;",
      "}",
      "",
    ].join("\n"),
  });

  assert.deepEqual(Object.values(results)[0], ["react-hooks(rules-of-hooks)"]);
});

test("enforces type-aware rules in TypeScript source", () => {
  const results = lintProbes(
    {
      "packages/design/src/probe.ts": [
        "async function load(): Promise<number> {",
        "  return 1;",
        "}",
        "load();",
        "export const label = `${{}}`;",
        "",
      ].join("\n"),
      "packages/design/tsconfig.json": JSON.stringify({
        compilerOptions: { strict: true, module: "ESNext", target: "ES2022" },
        include: ["src"],
      }),
    },
    { typeAware: true },
  );

  assert.deepEqual(results["packages/design/src/probe.ts"], [
    "typescript(no-base-to-string)",
    "typescript(no-floating-promises)",
    "typescript(restrict-template-expressions)",
  ]);
});

const lock = JSON.parse(
  readFileSync(path.join(repositoryRoot, ".repo-standards/lock.json"), "utf8"),
);
const excludedFiles = [
  ...Object.keys(lock.files),
  "apps/landing/src/api/generated/polity.ts",
  "apps/web/public/mockServiceWorker.js",
  "apps/web/src/app/i18n/generated/en/messages.ts",
  "apps/web/src/routeTree.gen.ts",
  "packages/design/src/themes/brand.css",
  "packages/ui/registry-manifest.json",
  "packages/ui/src/components/ui/button.tsx",
];

test("Prettier excludes Canon-owned and generated files", async () => {
  const included = [];
  for (const file of excludedFiles) {
    const info = await prettier.getFileInfo(path.join(repositoryRoot, file), {
      ignorePath: [
        path.join(repositoryRoot, ".gitignore"),
        path.join(repositoryRoot, ".prettierignore"),
      ],
    });
    if (!info.ignored) included.push(file);
  }

  assert.deepEqual(included, []);
});

test("oxlint excludes Canon-owned and generated files", () => {
  const linted = new Set(
    execFileSync(oxlint, ["--debug=files", "."], {
      cwd: repositoryRoot,
      encoding: "utf8",
    })
      .split("\n")
      .map((line) => line.trim())
      .filter(Boolean)
      .map((line) =>
        path.relative(repositoryRoot, path.resolve(repositoryRoot, line)),
      ),
  );

  assert.ok(linted.has("apps/web/src/main.tsx"));
  assert.deepEqual(
    excludedFiles.filter((file) => linted.has(file)),
    [],
  );
});
