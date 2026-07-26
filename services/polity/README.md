# Polity Service

The product-owned backend for constitutional government.

The service owns Polity's domain behavior, API contract, persistence, permissions, and lifecycle
decisions. Shared authorization, identity, billing, and API mechanics come from
[Cardo](https://github.com/lutzseverino/cardo).

The service imports the published Cardo BOM at `0.1.0-rc.5`.

## Development

Run service checks from the repository root; Maven resolves Cardo from Maven Central:

```bash
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
- `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_BASE_URL`, and `KEYCLOAK_REALM` configure the issuer.
- `KEYCLOAK_POLITY_CLIENT_ID` and `KEYCLOAK_POLITY_CLIENT_SECRET` configure the outbound product
  client, `polity-outbound` by default. Attach the optional scopes `identity`, `billing`, and
  `cardo-invite`; each scope must emit only its identically named audience. Grant the service
  account only `identity:profile:read`, `billing:entitlement:read`, and
  `cardo-invite:product-service`, and configure Invite with
  `INVITE_PRODUCT_CLIENT_IDS=polity-outbound`.
- `KEYCLOAK_POLITY_CATALOG_CLIENT_ID` and `KEYCLOAK_POLITY_CATALOG_CLIENT_SECRET` configure the
  `polity` resource-server PAT for Authorization Services catalog protection; its service account
  holds `polity:uma_protection` and no realm-management roles.
  `KEYCLOAK_POLITY_REALM_ADMIN_CLIENT_ID` and
  `KEYCLOAK_POLITY_REALM_ADMIN_CLIENT_SECRET` configure the separate least-privilege credential used
  only for exact client lookup and assignment or removal of already-defined roles on users. In the
  standard Keycloak model it receives `view-clients` and `manage-users`, not catalog protection or
  contract-materialization authority. Verify the three credentials cannot perform one another's
  operations as described in the
  [membership invitation integration reference](docs/reference/membership-invitation-integration.md).
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
