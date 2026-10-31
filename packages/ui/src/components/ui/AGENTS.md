# Canonical registry source

Treat every file in this directory, except this file and `README.md`, as
read-only generated source.

- Do not create, edit, format, rename, or delete source files manually.
- Add a component with `pnpm --filter @polity/ui ui:add <component...>`.
- Refresh installed components with `pnpm --filter @polity/ui ui:update`.
- Put shared behavior and styling invariants in `../app/`, not here.
- Never refresh provenance independently of the pinned CLI workflow.

The canonical workflow records every generated file's checksum in
`registry-manifest.json`. `ui:check` fails when this directory differs from
that provenance manifest.
