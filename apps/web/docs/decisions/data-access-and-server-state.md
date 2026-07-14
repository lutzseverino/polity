# Data Access and Server State

## Status

Accepted

## Context

The web client needs one predictable path from product code to the Polity service. The first governing
journey still uses fixture-backed adapters while authentication and the complete transport mapping are
being integrated, but its consumers should not depend on fixture names or synchronous access patterns.
The service can localize response text, so the active locale affects both requests and cache identity.

TanStack Router already owns route loading and preloading. Adding TanStack Query should preserve that early
loading behavior while giving domains shared caching and features mutation orchestration.

## Decision

Use `src/api/` for shared transport mechanics. The Axios client factory owns the service base URL,
requires an accepted language for every request, applies `Accept-Language`, and returns response data rather
than exposing `AxiosResponse` to product code. Authentication and normalized error policy can be added at
this boundary when their contracts are available.

Keep plain asynchronous operations with their product owner:

- domain `api/*-requests.ts` modules own reusable reads for nouns such as polities, memberships, and inbox
  items;
- feature `api/*-request.ts` modules own user actions such as accepting an invitation, casting a vote, and
  responding to a nomination;
- current fixture adapters remain private implementation details behind those operations until their HTTP
  mappings can replace them without changing consumers.

Use TanStack Query directly rather than introducing an application query wrapper. Owner-local query and
mutation modules define stable keys, `queryOptions` or `mutationOptions`, and thin semantic hooks. Query keys
include locale whenever the response can contain localized data.

Route loaders call `queryClient.ensureQueryData` with the same options used by components. Components read
and subscribe through the owner hook, so navigation preloads data without creating a second cache or a
post-render request waterfall. The application Query client provides a brief default freshness window to
avoid immediately repeating a loader request; an owner can override it when its data has different
freshness requirements.

## Consequences

- Product code no longer imports Axios or fixture functions directly.
- Query keys, request functions, loader preloading, and component subscriptions remain colocated by owner.
- Locale changes select a distinct cache entry instead of briefly presenting data in the previous language.
- Replacing fixture-backed operations with HTTP is an internal owner change, not a route or component
  migration.
- Cross-cutting query behavior belongs in `QueryClient`, while transport behavior belongs in the HTTP
  client; neither requires a generic hook API.
- The source architecture check prevents Axios and TanStack Query from spreading outside their boundaries.

## Alternatives Considered

- A generic application query hook: rejected because it would mirror TanStack Query's evolving API, weaken
  direct use of query options in loaders, and create an arbitrary application framework.
- Route loaders as the only cache: rejected because polity, membership, and inbox data are shared across the
  shell and multiple routes and will need mutation invalidation.
- Hooks that call Axios directly: rejected because transport calls would become React-only and unavailable
  to loaders, tests, and other non-component consumers.
- Immediate replacement of fixtures with authenticated HTTP: deferred because the web app does not yet own
  the bearer-token contract or complete mapping from service responses to its richer presentation models.
