<div align="center">
    <h1 align="center">Polity</h1>
    <p>Constitutional government software for small groups.</p>
    <p>
        <a href="https://github.com/lutzseverino/polity/actions/workflows/check-js.yml"><img alt="Check JavaScript" src="https://github.com/lutzseverino/polity/actions/workflows/check-js.yml/badge.svg"></a>
        <a href="https://github.com/lutzseverino/polity/actions/workflows/check-service.yml"><img alt="Check Service" src="https://github.com/lutzseverino/polity/actions/workflows/check-service.yml/badge.svg"></a>
        <a href="LICENSE"><img alt="MIT License" src="https://img.shields.io/badge/license-MIT-blue.svg"></a>
    </p>
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
| `packages/design` | Shared product design system |
| `services/polity` | Spring Boot product service and OpenAPI contract |
| `docs` | Repository-level durable documentation |

## Development

Install Cardo into your local Maven repository, then install the JavaScript workspace:

```bash
mvn -f ../cardo/pom.xml install
pnpm install
```

Run product surfaces and checks from the repository root:

```bash
pnpm dev:landing
pnpm dev:web
pnpm dev:mobile
pnpm check
pnpm quality
```

The pnpm install unit is the workspace. Run installs from the root so workspace dependencies are
linked consistently.

## Documentation

Start with the [documentation index](docs/README.md). Documentation is organized by reader intent
so durable guidance has one predictable home. Each application, package, and the
[Polity service](services/polity/README.md) keep owned guidance beside their code.

## License

Polity is available under the [MIT License](LICENSE).
