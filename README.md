<div align="center">
  <h1>Polity</h1>
  <p>Constitutional government software for small groups.</p>

  [![CI](https://github.com/lutzseverino/polity/actions/workflows/ci.yml/badge.svg)](https://github.com/lutzseverino/polity/actions/workflows/ci.yml)
  [![License: MIT](https://img.shields.io/badge/license-MIT-2f3437)](LICENSE)
</div>

Polity is a product workspace for membership, constitutions, institutions, offices, motions,
voting, certification, and official records. Its backend consumes the reusable identity,
authorization, and billing capabilities provided by [Cardo](https://github.com/lutzseverino/cardo).

## Workspace

| Path | Owner |
| --- | --- |
| `apps/landing` | Public product site |
| `apps/web` | React product application |
| `apps/mobile` | Expo mobile application |
| `packages/design` | Framework-neutral design tokens and browser theme outputs |
| `packages/ui` | Shared React components and canonical shadcn registry source |
| `services/polity` | Spring Boot product service and OpenAPI contract |
| `deploy` | Environment-neutral runtime images and same-origin gateway |
| `docs` | Repository-level durable documentation |

## Development

Cardo Java artifacts resolve from Maven Central through the service's published BOM. Install the
JavaScript workspace:

```bash
pnpm install
```

Run product surfaces and checks from the repository root:

```bash
pnpm dev:landing
pnpm dev:web
pnpm dev:mobile
pnpm check
```

`pnpm check` is the canonical gate. It runs workflow, formatting, lint, generated-artifact, type,
architecture, production-build, web and service test, SpotBugs, and dependency-hygiene checks.

The pnpm install unit is the workspace. Run installs from the root so workspace dependencies are
linked consistently.

Required CI uses Cardo `0.1.0-rc.5` from Maven Central. The scheduled `Cardo Compatibility`
workflow installs Cardo `main` explicitly as `0.1.0-SNAPSHOT` and verifies Polity with that override,
without changing the required release baseline.

## Documentation

Start with the [documentation index](docs/README.md). Documentation is organized by reader intent
so durable guidance has one predictable home. Each application, package, and the
[Polity service](services/polity/README.md) keep owned guidance beside their code.

## License

Polity is available under the [MIT License](LICENSE).
