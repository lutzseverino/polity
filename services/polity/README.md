# Polity Service

The product-owned backend for constitutional government.

The service owns Polity's domain behavior, API contract, persistence, permissions, and lifecycle
decisions. Shared authorization, identity, billing, and API mechanics come from
[Cardo](https://github.com/lutzseverino/cardo).

Durable authorization-grant receipts require Cardo revision
`07264c9603ede38b233c570c5343b1c34dbf553d` from
[Cardo PR #18](https://github.com/lutzseverino/cardo/pull/18), which is also pinned in CI.

## Development

Install Cardo into the local Maven repository, then run service checks from the repository root:

```bash
../cardo/mvnw -f ../cardo/pom.xml install
pnpm test:service
pnpm check:service:architecture
pnpm check:service:static
```

## Documentation

Start with the [documentation index](docs/README.md). Documentation is organized by reader intent
so durable guidance has one predictable home. The [OpenAPI contract](openapi/polity.yaml) is owned
beside the service.
