# Development

Build, test and maintain the Polity workspace.

## Setup and validation

Use Node.js 24 or 26, pnpm 11.28.9 (the root `packageManager` pin), Java 21
and the checked-in Maven wrapper. Run commands from the repository root.
Install the JavaScript workspace with `pnpm install --frozen-lockfile` from the
root `pnpm-lock.yaml`. Maven resolves the published Cardo 0.1.0-rc.5 baseline
from Maven Central.

Run `pnpm dev:web`, `pnpm dev:landing`, or `pnpm dev:mobile` for the selected
application. `pnpm check` is the canonical validation gate: repository workflow
and runtime pins, JavaScript validation, Maven verification and SpotBugs, and
knip dependency hygiene. Required CI runs the JavaScript validation and
dependency hygiene on Node.js 24 and 26 and also smoke-tests runtime images.
The service's integration tests and runtime smoke tests need Docker.

`pnpm check:js` runs, in order:

- `pnpm format:check` and `pnpm lint`: Prettier 3 and oxlint 1, with
  type-aware rules, across the root, the browser Projects, the shared packages
  and mobile sources; `pnpm format` applies Prettier. Mobile also keeps Expo's
  own ESLint through `expo lint`.
- `pnpm test:tooling`: checks of the lint rules that guard the web HTTP
  boundary, app-owned UI wrappers, hooks and type-aware rules, of both tools'
  exclusion of Repo Canon-owned and generated files, and of the UI registry
  integrity check.
- The `validate` script of each web Project on the Vite toolchain:
  - `packages/design`: formatting, lint, TypeScript 7, Dependency Cruiser and
    the generated theme check (`check:theme`).
  - `packages/ui`: formatting, lint, TypeScript 7, Dependency Cruiser and the
    offline registry integrity check (`ui:check`).
  - `apps/web`: formatting, lint, route and message generation with
    TypeScript 7, Vitest 5, Dependency Cruiser and the production build.
- Landing's TypeScript check, architecture check and build, and mobile's
  TypeScript and architecture checks.

Landing keeps its own TypeScript 5.9 compiler, and mobile keeps Expo's
TypeScript 6. Dependency Cruiser reads TypeScript through the root
TypeScript 5.9, because TypeScript 7 publishes no compiler API.

oxlint's correctness rules replace Biome's recommended lint rules, and the
typed, hooks, react-refresh and restricted-import rules carry over from
ESLint. Some coverage has no replacement:

- Prettier does not sort or group imports, as Biome's import organizer did.
- Neither tool lints CSS or JSON; Prettier still formats them.
- Biome recommended rules without an oxlint equivalent no longer run.
- `react/set-state-in-effect` and `typescript/no-redundant-type-constituents`
  are off: ESLint never enforced them and existing code violates them.

Repo Canon's installed checks run with
`.repo-standards/runtime/node_modules/.bin/repo-standards check`.

## Index

No additional shared development documents are maintained here. Project-specific
instructions are linked from each Project README.
