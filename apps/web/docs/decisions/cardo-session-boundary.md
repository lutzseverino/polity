# Cardo Session Boundary

## Status

Accepted

## Context

The web client needs to restore, establish, refresh, and end Cardo Identity sessions without storing
Identity or product credentials in JavaScript state. Public invitation-token onboarding must remain usable
without protected reads, while authenticated routes need one fail-closed entry boundary and a safe way to
resume local destinations.

## Decision

The root router classifies only `/sign-in` and invitation-token paths as public. Public paths skip the
application shell loader. Every other path ensures the owner-local current-session query before protected
shell and route loaders run, redirecting an unrefreshable session to `/sign-in`.

The `session` domain validates Cardo's principal response and owns current-session reads, query state, and a
single-flight refresh. Restoration performs one current-session read, at most one coordinated refresh, and
one final current-session read. Product mutations are never replayed.

Sign-in and sign-out features own their action requests and stable UI states. Sign-in bootstraps Cardo CSRF
before posting credentials. The shared HTTP client reads `cardo.csrf` or `__Host-cardo.csrf` and echoes it as
`X-CSRF-TOKEN` only on unsafe requests when present. Access and refresh credentials remain HTTP-only.

Return destinations must parse as same-origin paths beginning with `/`; absolute, protocol-relative,
backslash-normalized external, and sign-in destinations are rejected. Invitation onboarding hands completed
signup to sign-in with `/inbox` as the pending product destination.

Session-dependent TanStack Query entries opt in through `meta.requiresSession`. Sign-in, sign-out, and
terminal unauthorized transitions clear those entries while preserving public onboarding state. A terminal
unauthorized transition is coordinated so concurrent failures do not overwrite the first return destination.

## Consequences

- Browser mock and live modes use identical request modules and relative `/api/v1` URLs.
- Public invitation rendering issues no current-session, inbox, or polity reads.
- Refresh is constrained to session restoration and cannot create unsafe request replay.
- Product authorization convergence remains a backend dependency; the browser does not infer grants or
  store bearer tokens.

## Alternatives Considered

- A generic authentication provider: rejected because server session state already belongs in TanStack
  Query and the application needs no second client-state framework.
- Automatic replay after refresh: rejected because unsafe mutations cannot be replayed safely and Cardo's
  product-authorization convergence is intentionally outside this browser slice.
- Moving every protected route under a new pathless layout: rejected because the root boundary can skip
  public loading and guard protected paths without renaming the established route tree.
