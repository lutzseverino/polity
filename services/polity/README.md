# Polity Service

Polity owns constitutional government behavior, including membership, versioned constitutions,
procedures, motions, voting, certification, typed official effects, and the official record.

Platform authorization controls coarse product access. Constitutional authority remains Polity
domain state and is evaluated synchronously by Polity application services.

## Development

From the repository root:

```bash
pnpm test:service
pnpm compile:service
pnpm check:service:architecture
pnpm check:service:static
```

Or from this service directory:

```bash
cd services/polity
mvn test
```

The service consumes Odonta platform artifacts through the local or configured Maven repository.

Architecture rules live in the ArchUnit test suite and run with `mvn test`. SpotBugs runs through
`pnpm check:service:static`; generated OpenAPI code and low-value model DTO mutability findings are
excluded in `spotbugs-exclude.xml` so the gate focuses on higher-signal bug patterns.

## Documentation

Durable service docs live in [docs](docs/README.md).

- [Architecture](docs/explanation/architecture.md)
- [OpenAPI contract](openapi/polity.yaml)
