# Polity Service

The product-owned backend for constitutional government.

The service owns Polity's domain behavior, API contract, persistence, permissions, and lifecycle
decisions. Shared authorization, identity, billing, and API mechanics come from
[Cardo](https://github.com/lutzseverino/cardo).

## Development

Install Cardo into the local Maven repository, then run service checks from the repository root:

```bash
../cardo/mvnw -f ../cardo/pom.xml install
pnpm test:service
pnpm check:service:architecture
pnpm check:service:static
```

## Runtime configuration

The service is configured entirely through runtime inputs:

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` configure
  persistence.
- `POLITY_PORT` configures the HTTP port.
- `IDENTITY_BASE_URL`, `INVITE_BASE_URL`, and `BILLING_BASE_URL` configure Cardo service clients.
- `POLITY_MEMBERSHIP_INVITATION_ACCEPT_URL_BASE` and
  `POLITY_MEMBERSHIP_INVITATION_RETRY_DELAY` configure browser invitation links and retry timing.
- `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_BASE_URL`, `KEYCLOAK_REALM`,
  `KEYCLOAK_POLITY_CLIENT_ID`, and `KEYCLOAK_POLITY_CLIENT_SECRET` configure the current issuer and
  authorization provider integration.
- Cardo product-authentication inputs configure issuer, Identity-session audience, Polity audience,
  session and CSRF cookie names, token exchange, and active-token validation once the corresponding
  published Cardo module is adopted.

The service exposes `/actuator/health/liveness` and `/actuator/health/readiness`. It honors forwarded
headers and uses graceful shutdown so it can run behind an environment-neutral same-origin gateway.
Container packaging and routing requirements are documented in [Deployment](../../deploy/README.md).

## Documentation

Start with the [documentation index](docs/README.md). Documentation is organized by reader intent
so durable guidance has one predictable home. The [OpenAPI contract](openapi/polity.yaml) is owned
beside the service.
