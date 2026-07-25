import { HttpResponse, http } from "msw";
import { afterEach, describe, expect, it } from "vitest";

import {
  bootstrapSessionCsrf,
  getCurrentSession,
  readCurrentSession,
  refreshCurrentSession,
  renewCurrentSession,
} from "@/domains/session/api/session-requests";
import { isSessionUnavailableError } from "@/domains/session/lib/session-lifecycle";
import {
  browserSessionResponse,
  createSessionScenarioHandlers,
  renewedSessionPrincipalResponse,
  sessionPrincipalResponse,
} from "@/mocks/scenarios/session";
import { setTestCookie } from "@/test/cookies";
import { apiMockServer } from "@/test/mocks/server";

afterEach(() => {
  setTestCookie("cardo.csrf=; Max-Age=0; Path=/");
});

function endReason(error: unknown) {
  return isSessionUnavailableError(error) ? error.reason : undefined;
}

describe("session requests", () => {
  it("uses the CSRF bootstrap contract and accepted language", async () => {
    let acceptedLanguage: string | null = null;
    apiMockServer.use(
      http.get("/api/v1/identity/sessions/csrf", ({ request }) => {
        acceptedLanguage = request.headers.get("Accept-Language");
        return new HttpResponse(null, { status: 204 });
      }),
    );

    await bootstrapSessionCsrf({ acceptedLanguage: "es" });

    expect(acceptedLanguage).toBe("es");
  });

  it("reads and validates the current principal with its browser-session timing", async () => {
    const session = await getCurrentSession({ acceptedLanguage: "en" });

    expect(session).toEqual({
      authenticationMethod: "password",
      browserSession: browserSessionResponse,
      expiresAt: sessionPrincipalResponse.expiresAt,
      principal: {
        email: "member@example.com",
        emailVerified: true,
        id: "99999999-9999-4999-8999-999999999999",
        name: "Mira Chen",
      },
      sessionId: "mock-session",
    });
  });

  it("accepts a principal without browser-session metadata", async () => {
    apiMockServer.use(
      http.get("/api/v1/identity/sessions/current", () =>
        HttpResponse.json({
          ...sessionPrincipalResponse,
          browserSession: null,
        }),
      ),
    );

    const session = await getCurrentSession({ acceptedLanguage: "en" });

    expect(session.browserSession).toBeUndefined();
    expect(session.principal.email).toBe("member@example.com");
  });

  it("rejects malformed browser-session metadata at the transport boundary", async () => {
    apiMockServer.use(
      http.get("/api/v1/identity/sessions/current", () =>
        HttpResponse.json({
          ...sessionPrincipalResponse,
          browserSession: {
            ...browserSessionResponse,
            idleExpiresAt: "not-a-timestamp",
          },
        }),
      ),
    );

    await expect(getCurrentSession({ acceptedLanguage: "en" })).rejects.toThrow(
      /browserSession\.idleExpiresAt/,
    );
  });

  it("rejects a malformed current principal at the transport boundary", async () => {
    apiMockServer.use(
      http.get("/api/v1/identity/sessions/current", () =>
        HttpResponse.json({ user: { email: "member@example.com" } }),
      ),
    );

    await expect(getCurrentSession({ acceptedLanguage: "en" })).rejects.toThrow(
      /authenticationMethod/,
    );
  });

  it("renews the idle deadline through an explicit refresh", async () => {
    setTestCookie("cardo.csrf=mock-csrf-token; Path=/");
    apiMockServer.use(
      ...createSessionScenarioHandlers({ initialSession: "expired" }),
    );

    const session = await renewCurrentSession({ acceptedLanguage: "en" });

    expect(session.principal.email).toBe("member@example.com");
    expect(session.browserSession?.idleExpiresAt).toBe(
      renewedSessionPrincipalResponse.browserSession.idleExpiresAt,
    );
    expect(session.browserSession?.sessionStartedAt).toBe(
      browserSessionResponse.sessionStartedAt,
    );
    expect(session.browserSession?.absoluteExpiresAt).toBe(
      browserSessionResponse.absoluteExpiresAt,
    );
  });

  it("never refreshes while reading the current session", async () => {
    let refreshes = 0;
    setTestCookie("cardo.csrf=mock-csrf-token; Path=/");
    apiMockServer.use(
      // The counting handler must precede the scenario's own so it is the one that matches.
      http.post("/api/v1/identity/sessions/current/refresh", () => {
        refreshes += 1;
        return HttpResponse.json(renewedSessionPrincipalResponse);
      }),
      ...createSessionScenarioHandlers({ initialSession: "expired" }),
    );

    await expect(
      readCurrentSession({ acceptedLanguage: "en" }),
    ).rejects.toSatisfy(isSessionUnavailableError);
    expect(refreshes).toBe(0);
  });

  it.each([
    ["idle-expired", "session_idle_expired"],
    ["absolute-expired", "session_absolute_expired"],
    ["revoked", "session_revoked"],
  ] as const)("preserves the %s lifecycle reason", async (initialSession, reason) => {
    setTestCookie("cardo.csrf=mock-csrf-token; Path=/");
    apiMockServer.use(...createSessionScenarioHandlers({ initialSession }));

    await expect(
      readCurrentSession({ acceptedLanguage: "en" }),
    ).rejects.toSatisfy((error: unknown) => endReason(error) === reason);
  });

  it("reports an unstated rejection as unauthenticated so older Cardo stays compatible", async () => {
    apiMockServer.use(
      http.get("/api/v1/identity/sessions/current", () =>
        HttpResponse.json({}, { status: 401 }),
      ),
    );

    await expect(
      readCurrentSession({ acceptedLanguage: "en" }),
    ).rejects.toSatisfy(
      (error: unknown) => endReason(error) === "unauthenticated",
    );
  });

  it("reports a forbidden current session without refreshing", async () => {
    let refreshes = 0;
    apiMockServer.use(
      http.get("/api/v1/identity/sessions/current", () =>
        HttpResponse.json({}, { status: 403 }),
      ),
      http.post("/api/v1/identity/sessions/current/refresh", () => {
        refreshes += 1;
        return HttpResponse.json(renewedSessionPrincipalResponse);
      }),
    );

    await expect(
      readCurrentSession({ acceptedLanguage: "en" }),
    ).rejects.toSatisfy((error: unknown) => endReason(error) === "forbidden");
    expect(refreshes).toBe(0);
  });

  it("coordinates concurrent refreshes in one tab into a single rotation", async () => {
    let refreshes = 0;
    setTestCookie("cardo.csrf=mock-csrf-token; Path=/");
    apiMockServer.use(
      http.post("/api/v1/identity/sessions/current/refresh", () => {
        refreshes += 1;
        return HttpResponse.json(renewedSessionPrincipalResponse);
      }),
    );

    const sessions = await Promise.all([
      renewCurrentSession({ acceptedLanguage: "en" }),
      renewCurrentSession({ acceptedLanguage: "en" }),
      renewCurrentSession({ acceptedLanguage: "en" }),
    ]);

    expect(sessions).toHaveLength(3);
    expect(refreshes).toBe(1);
  });

  it("converges on a refresh another tab already owns", async () => {
    setTestCookie("cardo.csrf=mock-csrf-token; Path=/");
    apiMockServer.use(
      ...createSessionScenarioHandlers({ initialSession: "refresh-conflict" }),
    );

    const session = await renewCurrentSession({ acceptedLanguage: "en" });

    expect(session.principal.email).toBe("member@example.com");
  });

  it("waits out a competing rotation that outlasts the first re-read", async () => {
    let reads = 0;
    setTestCookie("cardo.csrf=mock-csrf-token; Path=/");
    apiMockServer.use(
      http.get("/api/v1/identity/sessions/current", () => {
        reads += 1;
        // The competing tab publishes its rotated credential only on the third read.
        return reads < 3
          ? HttpResponse.json({}, { status: 401 })
          : HttpResponse.json(renewedSessionPrincipalResponse);
      }),
      http.post("/api/v1/identity/sessions/current/refresh", () =>
        HttpResponse.json(
          {
            error: {
              code: "session_refresh_in_progress",
              message: "The browser session is already being refreshed.",
            },
          },
          { status: 409 },
        ),
      ),
    );

    const session = await renewCurrentSession({ acceptedLanguage: "en" });

    expect(session.principal.email).toBe("member@example.com");
    expect(reads).toBe(3);
  });

  it("stops converging as soon as a read states a lifecycle reason", async () => {
    let reads = 0;
    setTestCookie("cardo.csrf=mock-csrf-token; Path=/");
    apiMockServer.use(
      http.get("/api/v1/identity/sessions/current", () => {
        reads += 1;
        return HttpResponse.json(
          {
            error: {
              code: "session_absolute_expired",
              message: "The browser session reached its absolute deadline.",
            },
          },
          { status: 401 },
        );
      }),
      http.post("/api/v1/identity/sessions/current/refresh", () =>
        HttpResponse.json(
          {
            error: {
              code: "session_refresh_in_progress",
              message: "The browser session is already being refreshed.",
            },
          },
          { status: 409 },
        ),
      ),
    );

    await expect(
      renewCurrentSession({ acceptedLanguage: "en" }),
    ).rejects.toSatisfy(
      (error: unknown) => endReason(error) === "session_absolute_expired",
    );
    expect(reads).toBe(1);
  });

  it("ends the session when a superseded refresh cannot converge", async () => {
    setTestCookie("cardo.csrf=mock-csrf-token; Path=/");
    apiMockServer.use(
      http.get("/api/v1/identity/sessions/current", () =>
        HttpResponse.json(
          {
            error: {
              code: "session_revoked",
              message: "The browser session was revoked.",
            },
          },
          { status: 401 },
        ),
      ),
      http.post("/api/v1/identity/sessions/current/refresh", () =>
        HttpResponse.json(
          {
            error: {
              code: "session_refresh_superseded",
              message: "The browser session refresh was superseded.",
            },
          },
          { status: 409 },
        ),
      ),
    );

    await expect(
      renewCurrentSession({ acceptedLanguage: "en" }),
    ).rejects.toSatisfy(
      (error: unknown) => endReason(error) === "session_revoked",
    );
  });

  it("returns the refreshed principal directly", async () => {
    setTestCookie("cardo.csrf=mock-csrf-token; Path=/");
    const session = await refreshCurrentSession({ acceptedLanguage: "en" });

    expect(session.principal.name).toBe("Mira Chen");
  });
});
