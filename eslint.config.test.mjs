import assert from "node:assert/strict";
import path from "node:path";
import test from "node:test";

import { ESLint } from "eslint";

const repositoryRoot = import.meta.dirname;
const eslint = new ESLint({
  overrideConfigFile: path.join(repositoryRoot, "eslint.config.mjs"),
});
const directFetchSource = 'void self.fetch("/api/example");';

test("rejects self.fetch in production web source", async () => {
  const [result] = await eslint.lintText(directFetchSource, {
    filePath: path.join(
      repositoryRoot,
      "apps/web/src/domains/membership/api/membership-requests.ts",
    ),
  });

  assert.deepEqual(
    result.messages.map(({ ruleId }) => ruleId),
    ["no-restricted-properties"],
  );
});

test("allows self.fetch in the shared boundary, tests, and mocks", async (t) => {
  const allowedPaths = [
    "apps/web/src/api/http-client.ts",
    "apps/web/src/api/http-client.test.ts",
    "apps/web/src/mocks/development-api-source.ts",
  ];

  for (const allowedPath of allowedPaths) {
    await t.test(allowedPath, async () => {
      const [result] = await eslint.lintText(directFetchSource, {
        filePath: path.join(repositoryRoot, allowedPath),
      });

      assert.deepEqual(result.messages, []);
    });
  }
});

const sharedUiSource = 'import { Button } from "@polity/ui/button";';

test("rejects shared UI imports outside app-owned wrappers", async () => {
  const [result] = await eslint.lintText(sharedUiSource, {
    filePath: path.join(
      repositoryRoot,
      "apps/web/src/domains/membership/index.ts",
    ),
  });

  assert.deepEqual(
    result.messages.map(({ ruleId }) => ruleId),
    ["no-restricted-imports"],
  );
});

test("allows app-owned wrappers to consume shared UI", async () => {
  const [result] = await eslint.lintText(sharedUiSource, {
    filePath: path.join(
      repositoryRoot,
      "apps/web/src/components/app/AppButton/AppButton.tsx",
    ),
  });

  assert.deepEqual(result.messages, []);
});
