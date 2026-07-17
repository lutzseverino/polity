# Polity Landing

The landing app is the public front door and onboarding surface for Polity.

It is part of the root workspace and depends on the local `@polity/design` package.

## Development

From the repository root:

```bash
pnpm install
pnpm dev:landing
pnpm build:landing
```

The install unit is the workspace, even when you only work on this app. Run installs from the root so
the local package link is resolved consistently.

## Documentation

Durable landing docs live in [docs](docs/README.md).
