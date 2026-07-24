# API Mocking

## Status

Accepted

## Context

The web client initially used fixture-backed request modules and an inline Axios adapter in transport
tests. Those techniques are useful while product flows are being shaped, but they bypass the HTTP boundary.
As the client grows, tests need to detect incorrect URLs, methods, headers, serialization, cancellation, and
response handling without depending on a running Polity service.

## Decision

Use Mock Service Worker (MSW) as the standard API-mocking boundary for Vitest and browser development.
Product code always makes the same HTTP-shaped requests. Tests use `msw/node`; browser development uses
`msw/browser` when no `VITE_API_URL` is configured, while a configured URL selects the live proxy.
`VITE_API_MOCKING=true` forces browser mocking even when that URL exists. Application bootstrap awaits
worker startup before React renders, so initial route-loader requests cannot race ahead of interception.
Production builds do not activate or register the worker.

The shared test setup starts one Node mock server before the suite, rejects unhandled requests, recreates
default state and handlers after every test, and closes the server after the suite. The browser worker fails
unhandled same-origin `/api/` requests while allowing Vite assets and other non-API resources through.

Tests install the handlers for the scenario they exercise with `apiMockServer.use`. Keep a handler in its
test or development scenario while it has only one consumer. Extract only after actual reuse:

- Name transport-shaped reusable values `<noun>Response` and keep them beside the scenario that owns them.
- Name mutable in-memory values `<noun>State`; create them inside a scenario factory so browser reloads and
  test resets start predictably without local storage or other production persistence.
- Name scenario factories `create<Scenario>Handlers`; they own related response data, state transitions,
  and handlers. Put them under `src/mocks/scenarios/` only when browser development or multiple tests reuse
  the complete HTTP scenario.
- Keep one-off request handlers in their test. Do not grow a global catch-all handler catalog.

Mock handlers must not import application composition, routes, domains, features, or presentation owners.
Product modules are not mocked when the behavior under test crosses the HTTP boundary, and they never select
between real and mock request implementations.

The shared browser scenarios cover current-user invitations and the governing journey. The latter provides
transport-shaped polity directory, workspace, action, government, motion, record, vote, and candidacy
resources. Vote and candidacy handlers update their in-memory motion responses, so subsequent reads observe
the recorded choice. A separate stateful session scenario covers signed-out, signed-in, expired, revoked,
invalid-credential, refresh, and logout behavior through the Cardo URLs. The account scenario models
first-time provisioning, immediate application, pending-to-applied convergence, durable failure, and
subsequent reads without changing product request code. Future scenarios extend this boundary; application
request code remains unchanged.

Service-worker mocked responses cannot write browser cookies. Browser mock installation therefore
materializes the development scenario's readable CSRF cookie before the worker starts. This remains inside
`src/mocks/`; live and production startup depend exclusively on Cardo's `Set-Cookie` response. Because the
service-worker request view may also omit the forbidden `Cookie` header, browser handlers require the known
scenario CSRF header and compare the cookie too whenever that header is visible. Shared transport tests
separately prove that application code derives the header from the readable cookie.

## Consequences

- API-facing tests exercise the same Axios and HTTP behavior used by product code.
- Any unexpected network request fails the test instead of reaching a real service or passing silently.
- Per-test handlers keep scenarios explicit, and automatic resets prevent state from leaking between tests.
- Browser development can exercise pending, completed, failed, retry, and acceptance flows without a live
  service by default, while configuring `VITE_API_URL` sends the identical relative requests through Vite
  unless the explicit mock flag overrides it.
- Stateful browser scenarios reset on reload and never write to production persistence.
- Pure domain logic and narrowly scoped component tests may still use direct values or function doubles when
  the HTTP boundary is outside the behavior under test.
- MSW and its worker script are development tooling; production startup never registers or activates them.

## Alternatives Considered

- Inline Axios adapters: rejected as the general standard because they couple tests to Axios internals and
  bypass the observable network contract.
- Mocking request modules with Vitest: retained only for tests intentionally below the API boundary; using it
  broadly would make integration tests unable to detect malformed requests.
- A running backend for every frontend test: rejected because it is slower, less deterministic, and belongs
  in a smaller contract or end-to-end test layer.
