# Deployment

Polity's browser topology uses one browser-visible same HTTPS origin for the web application, Polity
API, and Cardo Identity session routes. The web gateway image serves the single-page application and
preserves the externally visible request path while routing:

- `/api/v1/identity/sessions` and descendants to Cardo Identity;
- the remaining `/api/v1` requests to the Polity service;
- application routes to the web client.

This path split keeps Cardo's host-only session and CSRF cookies on the same origin as product
requests without exposing product tokens to the browser. TLS may terminate at this gateway or at an
upstream ingress, but the browser-visible origin must use HTTPS in production and forwarding headers
must be preserved.

Only the gateway belongs at the browser-facing edge. Keep the Polity and Cardo Identity upstreams on
a trusted application network so clients cannot bypass same-origin routing or supply forwarding
headers directly; the gateway overwrites the forwarded host, protocol, and client chain it passes on.

## Images

Build the service artifact after installing the published Cardo dependencies, then package it:

```bash
./mvnw -f services/polity/pom.xml package -DskipTests
docker build -f deploy/polity-service.Dockerfile -t polity-service .
docker build -f deploy/web-gateway.Dockerfile -t polity-web .
```

The gateway accepts environment-neutral upstream inputs:

| Variable | Default | Meaning |
| --- | --- | --- |
| `CARDO_IDENTITY_UPSTREAM` | `identity:8081` | Cardo Identity host and port, without a scheme or path |
| `POLITY_SERVICE_UPSTREAM` | `polity:8084` | Polity service host and port, without a scheme or path |
| `POLITY_WEB_PORT` | `8080` | Gateway listen and health-check port |

The service accepts the Spring, Cardo client, product-authentication, and port variables documented
in [the service readme](../services/polity/README.md). It exposes liveness and readiness at
`/actuator/health/liveness` and `/actuator/health/readiness`. The gateway exposes its own liveness at
`/healthz`; upstream readiness should be checked independently so routing failures remain
diagnosable.

Production Cardo Identity must use its production cookie mode and set its externally visible refresh
cookie path to `/api/v1/identity/sessions/current`. The repository intentionally does not contain DNS,
host addresses, TLS certificates, homelab, or WireGuard configuration.
