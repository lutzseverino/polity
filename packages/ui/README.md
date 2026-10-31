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

The package pins shadcn as an exact devDependency and uses the
`base-nova` style with lucide icons. Generated components import each other
through the package-private `#` aliases declared in `package.json` `imports`
and configured in `components.json`, so consumers' own `@/*` aliases never
apply to them. Run only these commands from the repository root:

```bash
pnpm --filter @polity/ui ui:add button
pnpm --filter @polity/ui ui:update
pnpm --filter @polity/ui ui:check
```

`ui:add` runs the pinned CLI for the named components and records them in
`registry-manifest.json`. `ui:update` runs the pinned CLI with `--overwrite`
for exactly the components the manifest records, never `--all`, and fails if
the refresh creates files the manifest does not record. Both leave the CLI
output unmodified and record the checksum of every file the CLI owns: the
files under `src/components/ui/`, and under `src/lib/` when the `lib` and
`utils` aliases receive registry helpers, except this package's maintained
`src/components/ui/AGENTS.md` and `src/components/ui/README.md`.

The CLI also installs the dependencies the registry declares. After each run,
keep only the changes the regenerated components need, meaning type-checking
or a build fails without them, and revert any other range bump in
`package.json` and `pnpm-lock.yaml`. A registry entry can declare packages its
component file never imports, or ask for a newer range than it needs.

`ui:check` runs offline. It compares the committed bytes with the manifest
and reports changed, missing and unexpected generated files without fetching
the registry.

Shared effects on generated components live in `src/styles.css` as
`data-slot` rules inside `@layer components`, below Tailwind's utilities, so
application `className`s still win. Never edit generated files instead.

## Validation

```bash
pnpm --filter @polity/ui validate
```

`validate` runs Prettier, oxlint, TypeScript 7, Dependency Cruiser and
`ui:check`. The root `pnpm test:tooling` covers the registry helper itself.

For shared prerequisites and validation, read the [development guide](../../docs/development/README.md) and [contribution guidelines](../../CONTRIBUTING.md).
