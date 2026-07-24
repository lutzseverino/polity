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
application shell loader. Every other path ensures the owner-local current-session query and then the
separate Polity account-convergence query before protected shell and route loaders run, redirecting an
unrefreshable session to `/sign-in`.

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

The `account` domain validates Polity's durable account receipt and owns GET-first, POST-on-not-found
provisioning. The root waits at a fixed interval while the stored receipt is pending and reads only the
existing account during that wait. Applied convergence unlocks protected route loading. Failed convergence
is terminal: it stops polling, does not restage the receipt, and offers sign-out rather than an invented
repair action. Account convergence remains separate from both session state and future membership-access
convergence.

## Consequences

- Browser mock and live modes use identical request modules and relative `/api/v1` URLs.
- Public invitation rendering issues no current-session, inbox, or polity reads.
- Public routes issue no Polity account reads or provisioning requests.
- Protected product reads cannot race ahead of baseline account grant convergence.
- Refresh is constrained to session restoration and cannot create unsafe request replay.
- The browser observes Polity's durable convergence state without inferring grants, restaging failed work,
  or storing bearer tokens.

## Alternatives Considered

- A generic authentication provider: rejected because server session state already belongs in TanStack
  Query and the application needs no second client-state framework.
- Automatic replay after refresh: rejected because unsafe mutations cannot be replayed safely and Cardo's
  product-authorization convergence is intentionally outside this browser slice.
- Moving every protected route under a new pathless layout: rejected because the root boundary can skip
  public loading and guard protected paths without renaming the established route tree.
