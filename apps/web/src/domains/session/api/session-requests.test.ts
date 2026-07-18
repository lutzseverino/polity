import { HttpResponse, http } from "msw";
import { afterEach, describe, expect, it } from "vitest";

import {
  bootstrapSessionCsrf,
  getCurrentSession,
  isSessionUnavailableError,
  refreshCurrentSession,
  restoreCurrentSession,
} from "@/domains/session/api/session-requests";
import {
  createSessionScenarioHandlers,
  sessionPrincipalResponse,
} from "@/mocks/scenarios/session";
import { setTestCookie } from "@/test/cookies";
import { apiMockServer } from "@/test/mocks/server";

afterEach(() => {
  setTestCookie("cardo.csrf=; Max-Age=0; Path=/");
});

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

  it("reads and validates the current principal", async () => {
    const session = await getCurrentSession({ acceptedLanguage: "en" });

    expect(session).toEqual({
      authenticationMethod: "password",
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

  it("refreshes an expired session through the accepted endpoint", async () => {
    setTestCookie("cardo.csrf=mock-csrf-token; Path=/");
    apiMockServer.use(
      ...createSessionScenarioHandlers({ initialSession: "expired" }),
    );

    const session = await restoreCurrentSession({ acceptedLanguage: "en" });

    expect(session.principal.email).toBe("member@example.com");
  });

  it("coordinates concurrent refreshes and retries each current-session read once", async () => {
    let accessValid = false;
    let currentReads = 0;
    let refreshes = 0;
    setTestCookie("cardo.csrf=mock-csrf-token; Path=/");
    apiMockServer.use(
      http.get("/api/v1/identity/sessions/current", () => {
        currentReads += 1;
        return accessValid
          ? HttpResponse.json(sessionPrincipalResponse)
          : HttpResponse.json({}, { status: 401 });
      }),
      http.post("/api/v1/identity/sessions/current/refresh", () => {
        refreshes += 1;
        accessValid = true;
        return HttpResponse.json(sessionPrincipalResponse);
      }),
    );

    const sessions = await Promise.all([
      restoreCurrentSession({ acceptedLanguage: "en" }),
      restoreCurrentSession({ acceptedLanguage: "en" }),
      restoreCurrentSession({ acceptedLanguage: "en" }),
    ]);

    expect(sessions).toHaveLength(3);
    expect(refreshes).toBe(1);
    expect(currentReads).toBe(6);
  });

  it("treats a revoked session as terminal", async () => {
    setTestCookie("cardo.csrf=mock-csrf-token; Path=/");
    apiMockServer.use(
      ...createSessionScenarioHandlers({ initialSession: "revoked" }),
    );

    await expect(
      restoreCurrentSession({ acceptedLanguage: "en" }),
    ).rejects.toSatisfy(isSessionUnavailableError);
  });

  it("treats a forbidden current session as signed out without refreshing", async () => {
    let refreshes = 0;
    apiMockServer.use(
      http.get("/api/v1/identity/sessions/current", () =>
        HttpResponse.json({}, { status: 403 }),
      ),
      http.post("/api/v1/identity/sessions/current/refresh", () => {
        refreshes += 1;
        return HttpResponse.json(sessionPrincipalResponse);
      }),
    );

    await expect(
      restoreCurrentSession({ acceptedLanguage: "en" }),
    ).rejects.toSatisfy(isSessionUnavailableError);
    expect(refreshes).toBe(0);
  });

  it("returns the refreshed principal directly", async () => {
    setTestCookie("cardo.csrf=mock-csrf-token; Path=/");
    const session = await refreshCurrentSession({ acceptedLanguage: "en" });

    expect(session.principal.name).toBe("Mira Chen");
  });
});
