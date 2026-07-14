# API Mocking for Tests

## Status

Accepted

## Context

The web client initially used fixture-backed request modules and an inline Axios adapter in transport
tests. Those techniques are useful while product flows are being shaped, but they bypass the HTTP boundary.
As the client grows, tests need to detect incorrect URLs, methods, headers, serialization, cancellation, and
response handling without depending on a running Polity service.

## Decision

Use Mock Service Worker (MSW) as the standard API-mocking boundary for Vitest. The shared test setup starts
one Node mock server before the suite, rejects unhandled requests, resets runtime handlers after every test,
and closes the server after the suite.

Tests install the handlers for the scenario they exercise with `apiMockServer.use`. Keep a handler in its
test while it has only one consumer. Extract reusable response data or handler factories under
`src/test/mocks/` only after multiple tests share the same API scenario. Product modules are not mocked when
the behavior under test crosses the HTTP boundary.

The fixture-backed product request modules remain a temporary runtime adapter until their real HTTP and
authentication mappings are available. They are not the test-mocking standard.

## Consequences

- API-facing tests exercise the same Axios and HTTP behavior used by product code.
- Any unexpected network request fails the test instead of reaching a real service or passing silently.
- Per-test handlers keep scenarios explicit, and automatic resets prevent state from leaking between tests.
- Pure domain logic and narrowly scoped component tests may still use direct values or function doubles when
  the HTTP boundary is outside the behavior under test.
- Browser development mocks can later reuse handler factories, but this decision does not enable a service
  worker in production or development builds.

## Alternatives Considered

- Inline Axios adapters: rejected as the general standard because they couple tests to Axios internals and
  bypass the observable network contract.
- Mocking request modules with Vitest: retained only for tests intentionally below the API boundary; using it
  broadly would make integration tests unable to detect malformed requests.
- A running backend for every frontend test: rejected because it is slower, less deterministic, and belongs
  in a smaller contract or end-to-end test layer.
