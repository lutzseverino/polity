# Polity Design

Shared design tokens and theme-generation tooling for Polity product clients.

## Development

Run from the repository root:

```bash
pnpm --filter @polity/design typecheck
pnpm check:ts:architecture:design
```

Generate the shadcn theme consumed by product clients:

```bash
pnpm --filter @polity/design generate:shadcn-theme
```

## Structure

- `src/tokens.json` is the source of truth for portable design tokens.
- `src/tokens.ts` exposes typed token data.
- `src/index.ts` defines the package's public TypeScript surface.
- `bin/polity-design.mjs` owns generated theme output.

## Documentation

Durable design package docs live in [docs](docs/README.md).
