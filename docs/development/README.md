# Development

Build, test and maintain the Polity workspace.

## Setup and validation

Use Node.js 24, pnpm 9.12.0 (the root packageManager pin), Java 21 and the
checked-in Maven wrapper. Run commands from the repository root. Install the
JavaScript workspace with `pnpm install --frozen-lockfile`. Maven resolves the
published Cardo 0.1.0-rc.5 baseline from Maven Central.

Run `pnpm dev:web`, `pnpm dev:landing`, or `pnpm dev:mobile` for the selected
application. `pnpm check` is the canonical validation gate: repository workflow
and runtime pins, Biome/ESLint, generated theme and registry integrity,
TypeScript, import architecture, web tests and builds, Maven verification and
SpotBugs, and dependency hygiene. Required CI also smoke-tests runtime images.
The service's integration tests and runtime smoke tests need Docker.

Repo Canon's installed checks run with
`.repo-standards/runtime/node_modules/.bin/repo-standards check`.
Existing Projects retain their own tooling; no optional stack is selected.

## Index

No additional shared development documents are maintained here. Project-specific
instructions are linked from each Project README.
