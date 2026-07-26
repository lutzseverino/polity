# Product Authentication

Cardo owns authentication at the Polity API. Polity owns which routes are public or authenticated
and whether an authenticated principal may perform a product-domain action. This document is
authoritative for the request boundary and its configuration surface.

## Structure

Cardo's `identity-product-auth` contributes the single product filter chain and denies any request
no rule matched. It also supplies the shared authority conversion, permission evaluation,
authenticated-user reading, and method-security wiring that Polity previously declared itself.
Polity contributes exactly one `ProductRequestPolicy` bean and no security filter chain.

Both call styles converge on the same product-token validation and authority construction.

### Browser cookie requests

A request carrying the Identity session cookie is authenticated from that cookie. Cardo validates
the session token against `identity-session-audience` exactly, exchanges it server-side for a token
whose audience is `product-audience`, and validates that token exactly. The exchanged token's
identity user must match the session's, or the request is rejected. The browser never receives or
stores a product token.

Unsafe cookie-authenticated requests require a matching CSRF token. Cardo reads it from the CSRF
cookie and accepts it only from a request header; it never issues or rotates that cookie.

### Explicit bearer requests

A request carrying an explicit `Authorization: Bearer` token is validated directly against
`product-audience` and never falls back to the cookie path. CSRF does not apply, because the
protection matcher only engages when the session cookie selects the request.

Identity-session tokens presented as bearer tokens are rejected, as are wrong-audience and
multi-audience tokens, because both audiences are matched exactly.

## Rules

- Route policy is method-aware. The API supports `GET`, `POST`, and `PUT`; any other method on an
  API path is denied at the boundary rather than reaching a controller.
- Public routes are the health and information endpoints, the API documentation routes, the
  invitation-token lookup, and invitation completion. Everything else under the API base path
  requires authentication.
- Container probes read `/actuator/health/liveness` and `/actuator/health/readiness`. Cardo permits
  only `/actuator/health` and `/actuator/info`, so the policy permits the probe group explicitly.
- `/polity/account` and `/polities/{polityId}/members/me/access` are authenticated recovery
  surfaces. They authenticate the principal and must never require the product grant they exist to
  converge.
- Polity's permission vocabulary, grant plans, method annotations, and domain authorization
  decisions are unchanged; they run after authentication through method security.
- Polity must not customize Cardo's bearer resolver or CSRF repository as an alternate security
  path.

## Configuration

All keys sit under `cardo.identity.product-auth`.

| Key | Environment variable | Default |
| --- | --- | --- |
| `identity-session-audience` | `CARDO_IDENTITY_SESSION_AUDIENCE` | none; required |
| `product-audience` | `CARDO_POLITY_PRODUCT_AUDIENCE` | none; required |
| `session-cookie-name` | `CARDO_SESSION_COOKIE_NAME` | `cardo.session` |
| `csrf-cookie-name` | `CARDO_CSRF_COOKIE_NAME` | `cardo.csrf` |
| `token-exchange.connect-timeout` | `CARDO_PRODUCT_TOKEN_CONNECT_TIMEOUT` | `PT2S` |
| `token-exchange.read-timeout` | `CARDO_PRODUCT_TOKEN_READ_TIMEOUT` | `PT2S` |
| `active-token-validation.enabled` | `CARDO_ACTIVE_TOKEN_VALIDATION_ENABLED` | `false` |
| `active-token-validation.introspection-uri` | `CARDO_ACTIVE_TOKEN_INTROSPECTION_URI` | none |
| `active-token-validation.client-id` | `CARDO_ACTIVE_TOKEN_CLIENT_ID` | none |
| `active-token-validation.client-secret` | `CARDO_ACTIVE_TOKEN_CLIENT_SECRET` | none |
| `active-token-validation.cache-ttl` | `CARDO_ACTIVE_TOKEN_CACHE_TTL` | `PT10S` |
| `active-token-validation.cache-max-entries` | `CARDO_ACTIVE_TOKEN_CACHE_MAX_ENTRIES` | `2048` |
| `active-token-validation.connect-timeout` | `CARDO_ACTIVE_TOKEN_CONNECT_TIMEOUT` | `PT2S` |
| `active-token-validation.read-timeout` | `CARDO_ACTIVE_TOKEN_READ_TIMEOUT` | `PT2S` |

The two audiences are deliberately undefaulted. Cardo rejects a blank audience, and rejects an
identity-session audience equal to the product audience, at startup. A deployment that omits or
duplicates them fails closed rather than validating tokens against the wrong client.

Enabling active-token validation requires the introspection URI, client ID, and client secret;
Cardo validates that combination at startup.
