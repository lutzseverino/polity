# Polity Landing

The landing app is part of the root pnpm workspace and depends on `@polity/design` through the
workspace protocol.

From the repository root:

```bash
pnpm install
pnpm dev:landing
pnpm build:landing
```

The install unit is the workspace, even when you only work on this app. Running `npm install` inside
this folder will not resolve the workspace dependency correctly. Use the root pnpm commands instead.
