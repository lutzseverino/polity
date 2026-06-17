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
```

Or from this service directory:

```bash
cd services/polity
mvn test
```

The service consumes Odonta platform artifacts through the local or configured Maven repository.

## Documentation

Durable service docs live in [docs](docs/README.md).

- [Architecture](docs/explanation/architecture.md)
- [OpenAPI contract](openapi/polity.yaml)
