# Cardo Session Boundary

## Status

Accepted

## Context

The web client needs to restore, establish, refresh, and end Cardo Identity sessions without storing
Identity or product credentials in JavaScript state. Public invitation-token onboarding must remain usable
without protected reads, while authenticated routes need one fail-closed entry boundary and a safe way to
resume local destinations.

Cardo Identity owns browser-session lifetimes. Authentication, current-session, and refresh responses carry
an authoritative `browserSession` object with `serverTime`, `sessionStartedAt`, `idleExpiresAt`,
`absoluteExpiresAt`, and `refreshable`, while the top-level `expiresAt` means only the current Identity
authorization credential. Identity renews the idle deadline solely on an explicit refresh, states why a
session ended through stable codes, and answers a losing concurrent refresh with a conflict rather than by
ending the session. That object is absent for an explicit bearer response and for Identity versions that
predate the contract.

## Decision

The root router classifies only `/sign-in` and invitation-token paths as public. Public paths skip the
application shell loader. Every other path restores the owner-local current session before protected shell
and route loaders run, redirecting an unrestorable session to `/sign-in`.

The `session` domain validates Cardo's principal response and owns current-session reads, query state, the
browser-session timing, and a single-flight refresh. It carries `browserSession` as optional data and rejects
the object only when a present one is malformed, so bearer responses and older Identity versions stay
compatible. Product mutations are never replayed.

Reading and refreshing are separate operations because only a refresh renews inactivity. The current-session
query performs a plain read, so a background refetch or ordinary product traffic can never extend the idle
deadline. Restoration is the one intentional path that may refresh: it reuses a fresh cached session,
otherwise reads once, and escalates to a single coordinated refresh only when the authorization credential
expired while the browser session itself is still alive. The refresh response is authoritative, so no
confirming read follows it.

A session that ended keeps Cardo's reason. `session_idle_expired`, `session_absolute_expired`,
`session_revoked`, `refresh_credential_required`, and `refresh_credential_invalid` are preserved verbatim;
a rejection without a known code normalizes to `unauthenticated` or `forbidden`. Every stated reason is
terminal and skips the refresh entirely, because Identity has already closed the session. A
`session_refresh_in_progress` or `session_refresh_superseded` conflict is convergence rather than failure:
another browser tab owns that rotation, so the boundary re-reads the current session within a bounded number
of attempts and gives up only if the session is genuinely gone.

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
- Cardo's inactivity policy holds: a tab left open reading product data expires on schedule.
- The session domain can name why a session ended, which is what a later inactivity-warning surface needs.
  That surface is deliberately not built here.
- Product authorization convergence remains a backend dependency; the browser does not infer grants or
  store bearer tokens.
- Browser-session timing is verified against Cardo's merged contract through mock scenarios. Live
  same-origin verification waits for a published Cardo release that contains it; `0.1.0-rc.5` does not.

## Alternatives Considered

- A generic authentication provider: rejected because server session state already belongs in TanStack
  Query and the application needs no second client-state framework.
- Automatic replay after refresh: rejected because unsafe mutations cannot be replayed safely and Cardo's
  product-authorization convergence is intentionally outside this browser slice.
- Refreshing from the current-session query itself: rejected because every background refetch would renew
  the idle deadline and defeat the inactivity policy Identity now owns.
- Deriving idle and absolute deadlines in the browser from `expiresAt`: rejected because Identity is the
  session authority and browser clocks cannot be trusted; `serverTime` exists precisely to avoid that.
- Moving every protected route under a new pathless layout: rejected because the root boundary can skip
  public loading and guard protected paths without renaming the established route tree.
