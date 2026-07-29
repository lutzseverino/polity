# Polity UI

Shared React components for Polity browser applications.

## Ownership

- `src/components/ui/` is canonical shadcn registry source. It is generated,
  private to this package, and never imported directly by applications.
- `src/components/app/` is the package's public component surface. An unchanged
  primitive is a direct re-export. A wrapper is introduced only for a shared
  invariant and must preserve the primitive's props, ref, `className`,
  controlled state, and compound composition.
- Page, route, translation, feature, domain, and application-shell components
  remain in the application that owns them.

## Registry maintenance

The workspace pins shadcn in this package. Run only these commands from the
repository root:

```bash
pnpm --filter @polity/ui ui:add button
pnpm --filter @polity/ui ui:update
```

Both commands invoke the pinned CLI, normalize package-internal imports, and
refresh `registry-manifest.json`. `pnpm check` rejects manual or unrecorded
changes to canonical source.

## Validation

```bash
pnpm --filter @polity/ui check:registry
pnpm --filter @polity/ui typecheck
pnpm --filter @polity/ui check:architecture
```
