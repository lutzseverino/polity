/// <reference types="node" />

import { readdirSync, readFileSync } from "node:fs";
import path from "node:path";

import { describe, expect, it } from "vitest";

const sourceRoot = import.meta.dirname;
const legacyFocusIndicatorClasses =
  "focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/50";
const legacyDirectionalIndicatorClasses =
  "transition-transform group-hover:translate-x-0.5";

function findTsxFiles(directory: string): string[] {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const entryPath = path.join(directory, entry.name);

    if (entry.isDirectory()) {
      return findTsxFiles(entryPath);
    }

    return entry.isFile() && entry.name.endsWith(".tsx") ? [entryPath] : [];
  });
}

describe("web style conventions", () => {
  it("names mutation-owning feature components as workflows", () => {
    const featureRoot = path.join(sourceRoot, "features");
    const mutationOwningComponents = findTsxFiles(featureRoot)
      .filter((file) =>
        readFileSync(file, "utf8").match(/\/api\/[^"']*mutation/),
      )
      .map((file) => path.relative(featureRoot, file));

    expect(mutationOwningComponents).not.toHaveLength(0);
    for (const componentPath of mutationOwningComponents) {
      const componentName = path.basename(componentPath, ".tsx");

      expect(componentName).toMatch(/Workflow$/);
      expect(
        componentPath.endsWith(
          `components/${componentName}/${componentName}.tsx`,
        ),
      ).toBe(true);
      expect(
        readFileSync(path.join(featureRoot, componentPath), "utf8"),
      ).toContain(`export function ${componentName}`);
    }
  });

  it("owns the shared keyboard focus indicator in one semantic utility", () => {
    const styles = readFileSync(path.join(sourceRoot, "index.css"), "utf8");
    const legacyCallSites = findTsxFiles(sourceRoot)
      .filter((file) =>
        readFileSync(file, "utf8").includes(legacyFocusIndicatorClasses),
      )
      .map((file) => path.relative(sourceRoot, file));

    expect(styles).toContain("@utility focus-indicator");
    expect(styles).toContain("@apply outline-none ring-3 ring-ring/50;");
    expect(legacyCallSites).toEqual([]);
  });

  it("owns directional link-surface motion in one app component", () => {
    const legacyCallSites = findTsxFiles(sourceRoot)
      .filter((file) =>
        readFileSync(file, "utf8").includes(legacyDirectionalIndicatorClasses),
      )
      .map((file) => path.relative(sourceRoot, file));

    expect(legacyCallSites).toEqual([]);
  });
});
