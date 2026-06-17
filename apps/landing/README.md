# Polity Landing

The landing app is part of the root workspace and depends on the local `@polity/design` package.

From the repository root:

```bash
pnpm install
pnpm dev:landing
pnpm build:landing

npm install
npm run dev:landing
npm run build:landing
```

The install unit is the workspace, even when you only work on this app. Run installs from the root so
the local package link is resolved consistently.
