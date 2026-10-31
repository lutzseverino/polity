<div align="center">
  <h1>Polity</h1>
  <p>Constitutional government software for small groups.</p>
  <p>
    <a href="https://www.typescriptlang.org/"><img src="https://img.shields.io/badge/TypeScript-5.9-3178c6" alt="TypeScript 5.9" /></a>
    <a href="https://nodejs.org/"><img src="https://img.shields.io/badge/Node.js-24-5fa04e" alt="Node.js 24" /></a>
    <a href="https://openjdk.org/projects/jdk/21/"><img src="https://img.shields.io/badge/Java-21-007396" alt="Java 21" /></a>
    <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-2f3437" alt="MIT License" /></a>
    <a href="https://github.com/lutzseverino/polity/actions/workflows/ci.yml"><img src="https://github.com/lutzseverino/polity/actions/workflows/ci.yml/badge.svg" alt="CI" /></a>
  </p>
</div>

Polity helps groups manage membership, constitutions, offices, motions,
voting and official records. Its backend uses the shared identity,
authorization and billing services from [Cardo](https://github.com/lutzseverino/cardo).

## Features

- Manage group membership and invitations.
- Organize constitutional decisions and voting.
- Record certified outcomes through the web application and backend service.

## Usage

Run the web application against its in-memory development scenario to explore
membership invitations and voting workflows. Follow the
[web application guide](apps/web/README.md) for mock and live-service setup.
For self-hosting, follow the [runtime deployment guide](deploy/README.md).

## Workspace

Applications live in `apps/`: the public landing site, web client and dormant
Expo mobile shell. `packages/design` owns design tokens and themes;
`packages/ui` owns the shared React surface and shadcn registry.
`services/polity` owns the backend and OpenAPI contract.

## Documentation

[Documentation](docs/README.md)

## Contributing

[Contribution guidelines](CONTRIBUTING.md)

## License

[MIT License](LICENSE)
