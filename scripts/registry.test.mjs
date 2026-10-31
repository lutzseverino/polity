import assert from "node:assert/strict";
import {
  appendFileSync,
  cpSync,
  mkdtempSync,
  rmSync,
  unlinkSync,
  writeFileSync,
} from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import test from "node:test";

import { checkRegistry } from "../packages/ui/scripts/registry.mjs";

const packageRoot = path.resolve(import.meta.dirname, "../packages/ui");

// Runs the check against a copy of the committed package so each case can
// change generated output without touching the working tree.
function withPackageCopy(change) {
  const directory = mkdtempSync(path.join(tmpdir(), "polity-ui-registry-"));
  try {
    for (const entry of [
      "components.json",
      "package.json",
      "registry-manifest.json",
      "src",
    ]) {
      cpSync(path.join(packageRoot, entry), path.join(directory, entry), {
        recursive: true,
      });
    }
    change(directory);
    return checkRegistry(directory);
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
}

test("accepts the committed generated output", () => {
  assert.deepEqual(
    withPackageCopy(() => {}),
    [],
  );
});

test("rejects a changed generated file", () => {
  assert.deepEqual(
    withPackageCopy((directory) => {
      appendFileSync(
        path.join(directory, "src/components/ui/button.tsx"),
        "\n",
      );
    }),
    ["Changed generated file: src/components/ui/button.tsx"],
  );
});

test("rejects a missing generated file", () => {
  assert.deepEqual(
    withPackageCopy((directory) => {
      unlinkSync(path.join(directory, "src/components/ui/hooks/use-mobile.ts"));
    }),
    ["Missing generated file: src/components/ui/hooks/use-mobile.ts"],
  );
});

test("rejects unexpected files in generated directories", () => {
  assert.deepEqual(
    withPackageCopy((directory) => {
      writeFileSync(
        path.join(directory, "src/components/ui/extra.tsx"),
        "export {};\n",
      );
      cpSync(
        path.join(directory, "src/components/ui/button.tsx"),
        path.join(directory, "src/lib/utils.ts"),
      );
    }),
    [
      "Unexpected generated file: src/components/ui/extra.tsx",
      "Unexpected generated file: src/lib/utils.ts",
    ],
  );
});

test("leaves the maintained guidance files to their authors", () => {
  assert.deepEqual(
    withPackageCopy((directory) => {
      appendFileSync(
        path.join(directory, "src/components/ui/AGENTS.md"),
        "\nEdited.\n",
      );
      appendFileSync(
        path.join(directory, "src/components/ui/README.md"),
        "\nEdited.\n",
      );
    }),
    [],
  );
});

test("rejects a generator pin that differs from the manifest", () => {
  const problems = withPackageCopy((directory) => {
    writeFileSync(
      path.join(directory, "package.json"),
      JSON.stringify({ devDependencies: { shadcn: "4.14.0" } }),
    );
  });

  assert.deepEqual(problems, [
    "registry-manifest.json records shadcn 4.13.0, but package.json pins 4.14.0.",
  ]);
});
