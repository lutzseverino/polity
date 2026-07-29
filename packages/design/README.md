# Polity Design

Shared design tokens and theme-generation tooling for Polity product clients.

## Development

Run from the repository root:

```bash
pnpm --filter @polity/design typecheck
pnpm check:ts:architecture:design
```

Regenerate the package-owned brand theme from the framework-neutral tokens:

```bash
pnpm --filter @polity/design generate:theme
```

Browser clients consume the checked-in outputs directly:

```css
@import "@polity/design/brand.css";
@import "@polity/design/product.css";
```

## Structure

- `src/tokens.json` is the source of truth for portable design tokens.
- `src/tokens.ts` exposes typed token data.
- `src/index.ts` defines the package's public TypeScript surface.
- `src/themes/` exposes directly consumable browser theme outputs.
- `bin/polity-design.mjs` deterministically refreshes the brand output from tokens.

## Documentation

Durable design package docs live in [docs](docs/README.md).
