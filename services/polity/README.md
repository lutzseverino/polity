<div align="center">
    <h1 align="center">Polity Service</h1>
    <p>The product-owned backend for constitutional government.</p>
</div>

The service owns Polity's domain behavior, API contract, persistence, permissions, and lifecycle
decisions. Shared authorization, identity, billing, and API mechanics come from
[Cardo](https://github.com/lutzseverino/cardo).

## Development

Install Cardo into the local Maven repository, then run service checks from the repository root:

```bash
mvn -f ../cardo/pom.xml install
pnpm test:service
pnpm check:service:architecture
pnpm check:service:static
```

## Documentation

Start with the [documentation index](docs/README.md). Documentation is organized by reader intent
so durable guidance has one predictable home. The [OpenAPI contract](openapi/polity.yaml) is owned
beside the service.
