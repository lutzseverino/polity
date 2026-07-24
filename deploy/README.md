# Deployment

Polity's browser topology uses one browser-visible same HTTPS origin for the web application, Polity
API, and Cardo Identity session routes. The web gateway image serves the single-page application and
preserves the externally visible request path while routing:

- `/api/v1/identity/sessions` and descendants to Cardo Identity;
- the remaining `/api/v1` requests to the Polity service;
- application routes to the web client.

This path split keeps Cardo's host-only session and CSRF cookies on the same origin as product
requests without exposing product tokens to the browser. TLS may terminate at this gateway or at an
upstream ingress, but the browser-visible origin must use HTTPS in production.

Only the gateway belongs at the browser-facing edge. Keep the Polity and Cardo Identity upstreams on
a trusted application network so clients cannot bypass same-origin routing. The gateway constructs
the forwarded origin from explicit runtime inputs. It discards recognized client-supplied forwarding
metadata and replaces any forwarded-for chain with the gateway's transport peer. A deliberate
trusted-ingress address policy, if needed later, belongs to deployment work rather than this image.

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
| `CARDO_IDENTITY_UPSTREAM` | `identity:8081` | Cardo Identity `<host>:<port>`, without a scheme or path |
| `POLITY_EXTERNAL_HOST` | `localhost` | Browser-visible DNS hostname or IPv4 address, without a scheme or port |
| `POLITY_EXTERNAL_PORT` | `8080` | Browser-visible port from 1 through 65535 |
| `POLITY_EXTERNAL_SCHEME` | `http` | Browser-visible scheme, exactly `http` or `https` |
| `POLITY_SERVICE_UPSTREAM` | `polity:8084` | Polity service `<host>:<port>`, without a scheme or path |
| `POLITY_WEB_PORT` | `8080` | Gateway listen and health-check port from 1 through 65535 |

Set all three `POLITY_EXTERNAL_*` inputs together. Production normally uses `https`, the public
product host, and port `443`. These values remain authoritative when a trusted upstream terminates
TLS and connects to the gateway over HTTP, so the internal hop cannot downgrade the origin reported
to Identity or Polity. They are equally valid when TLS terminates at the gateway; TLS material and
listener configuration remain deployment-owned.

Before Nginx substitutes the template, the image validates every interpolated input and exits on the
first invalid value. Ports use the grammar `[0-9]+` and must have a decimal value from 1 through
65535. A host is either canonical dotted-decimal IPv4 (four `0`-to-`255` octets, without ambiguous
leading zeroes) or an ASCII DNS-style hostname: 1 to 253 characters, at least one letter, and
dot-separated 1-to-63-character labels that start and end with an ASCII letter or digit and otherwise
contain only ASCII letters, digits, or `-`. Upstreams use exactly `<host>:<port>` with those same host
and port rules. The grammar excludes whitespace, paths, schemes, Nginx syntax, trailing DNS dots, and
IPv6 literals. IPv6 is intentionally unsupported until the template can render and test its required
bracketed forms consistently.

The service accepts the Spring, Cardo client, product-authentication, and port variables documented
in [the service readme](../services/polity/README.md). It exposes liveness and readiness at
`/actuator/health/liveness` and `/actuator/health/readiness`. The gateway exposes its own liveness at
`/healthz`; upstream readiness should be checked independently so routing failures remain
diagnosable.

Production Cardo Identity must use its production cookie mode and set its externally visible refresh
cookie path to `/api/v1/identity/sessions/current`. The repository intentionally does not contain DNS,
host addresses, TLS certificates, homelab, or WireGuard configuration.

## Runtime smoke test

After packaging the Polity service JAR, build both runtime images and exercise the gateway boundary:

```bash
./deploy/test-runtime-images.sh
```

The test uses an isolated Docker network and mock HTTP upstreams. It verifies the rendered Nginx
configuration, gateway health, exact and descendant Identity session routing, Polity API routing,
SPA fallback, origin preservation across the internal HTTP hop, and rejection of spoofed forwarding
metadata. It also starts the built gateway with invalid and malicious values for every interpolated
input to verify that validation fails closed before template substitution.
