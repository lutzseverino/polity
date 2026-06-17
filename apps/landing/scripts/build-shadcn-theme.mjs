import { readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const dirname = path.dirname(fileURLToPath(import.meta.url));
const landingRoot = path.resolve(dirname, "..");
const repoRoot = path.resolve(landingRoot, "../..");
const tokensPath = path.join(repoRoot, "packages/design/src/tokens.json");
const outputPath = path.join(landingRoot, "src/design/tokens/shadcn-theme.css");
const checkOnly = process.argv.includes("--check");
const tokens = JSON.parse(readFileSync(tokensPath, "utf8"));

function cssToken(value) {
  return value.css;
}

function declarations(entries, indent = "  ") {
  return entries
    .map(([name, value]) => `${indent}--${name}: ${value};`)
    .join("\n");
}

function shadcnMode(mode) {
  const colors = tokens.colorModes[mode];
  const border = mode === "light" ? "var(--ink)" : cssToken(colors.border);
  const input = mode === "light" ? "var(--ink)" : cssToken(colors.input);

  return declarations([
    ["paper", cssToken(colors.paper)],
    ["ink", cssToken(colors.ink)],
    ["background", "var(--paper)"],
    ["foreground", "var(--ink)"],
    ["card", cssToken(colors.card)],
    ["card-foreground", "var(--ink)"],
    ["popover", cssToken(colors.popover)],
    ["popover-foreground", "var(--ink)"],
    ["primary", cssToken(colors.primary)],
    ["primary-foreground", cssToken(colors.primaryForeground)],
    ["secondary", cssToken(colors.secondary)],
    ["secondary-foreground", cssToken(colors.secondaryForeground)],
    ["muted", cssToken(colors.muted)],
    ["muted-foreground", cssToken(colors.mutedForeground)],
    ["accent", "var(--primary)"],
    ["accent-foreground", "var(--primary-foreground)"],
    ["destructive", cssToken(colors.destructive)],
    ["border", border],
    ["input", input],
    ["ring", "var(--primary)"],
    ["chart-1", "var(--primary)"],
    ["chart-2", "var(--secondary)"],
    ["chart-3", cssToken(colors.chart3)],
    ["chart-4", cssToken(colors.chart4)],
    ["chart-5", cssToken(colors.chart5)],
    ["sidebar", "var(--card)"],
    ["sidebar-foreground", "var(--ink)"],
    ["sidebar-primary", "var(--primary)"],
    ["sidebar-primary-foreground", "var(--primary-foreground)"],
    ["sidebar-accent", "var(--muted)"],
    ["sidebar-accent-foreground", "var(--ink)"],
    ["sidebar-border", "var(--border)"],
    ["sidebar-ring", "var(--ring)"],
  ]);
}

function buildTheme() {
  const themeEntries = [
    ["font-display", tokens.fonts.display],
    ["font-heading", tokens.fonts.heading],
    ["font-sans", tokens.fonts.sans],
    ["font-mono", tokens.fonts.mono],
    ["color-sidebar-ring", "var(--sidebar-ring)"],
    ["color-sidebar-border", "var(--sidebar-border)"],
    ["color-sidebar-accent-foreground", "var(--sidebar-accent-foreground)"],
    ["color-sidebar-accent", "var(--sidebar-accent)"],
    ["color-sidebar-primary-foreground", "var(--sidebar-primary-foreground)"],
    ["color-sidebar-primary", "var(--sidebar-primary)"],
    ["color-sidebar-foreground", "var(--sidebar-foreground)"],
    ["color-sidebar", "var(--sidebar)"],
    ["color-chart-5", "var(--chart-5)"],
    ["color-chart-4", "var(--chart-4)"],
    ["color-chart-3", "var(--chart-3)"],
    ["color-chart-2", "var(--chart-2)"],
    ["color-chart-1", "var(--chart-1)"],
    ["color-ring", "var(--ring)"],
    ["color-input", "var(--input)"],
    ["color-border", "var(--border)"],
    ["color-destructive", "var(--destructive)"],
    ["color-accent-foreground", "var(--accent-foreground)"],
    ["color-accent", "var(--accent)"],
    ["color-muted-foreground", "var(--muted-foreground)"],
    ["color-muted", "var(--muted)"],
    ["color-secondary-foreground", "var(--secondary-foreground)"],
    ["color-secondary", "var(--secondary)"],
    ["color-primary-foreground", "var(--primary-foreground)"],
    ["color-primary", "var(--primary)"],
    ["color-popover-foreground", "var(--popover-foreground)"],
    ["color-popover", "var(--popover)"],
    ["color-card-foreground", "var(--card-foreground)"],
    ["color-card", "var(--card)"],
    ["color-foreground", "var(--foreground)"],
    ["color-background", "var(--background)"],
    ["color-ink", "var(--ink)"],
    ["color-paper", "var(--paper)"],
    ["radius-sm", "calc(var(--radius) * 0.6)"],
    ["radius-md", "calc(var(--radius) * 0.8)"],
    ["radius-lg", "var(--radius)"],
    ["radius-xl", "calc(var(--radius) * 1.4)"],
    ["radius-2xl", "calc(var(--radius) * 1.8)"],
    ["radius-3xl", "calc(var(--radius) * 2.2)"],
    ["radius-4xl", "calc(var(--radius) * 2.6)"],
  ];

  return `${[
    "/* Generated from packages/design/src/tokens.json. */",
    "@custom-variant dark (&:is(.dark *));",
    "",
    "@theme inline {",
    declarations(themeEntries),
    "}",
    "",
    ":root {",
    `${shadcnMode("light")}\n  --radius: ${cssToken(tokens.radius.base)};`,
    "}",
    "",
    "@media (prefers-color-scheme: dark) {",
    "  :root:not(.light) {",
    shadcnMode("dark").replace(/^/gm, "  "),
    "  }",
    "}",
    "",
    ".dark {",
    shadcnMode("dark"),
    "}",
  ].join("\n")}\n`;
}

const next = buildTheme();

if (checkOnly) {
  const current = readFileSync(outputPath, "utf8");
  if (current !== next) {
    console.error(
      "Generated shadcn theme is out of date. Run `pnpm --dir apps/landing generate:theme`.",
    );
    process.exit(1);
  }
} else {
  writeFileSync(outputPath, next);
}
