import { QueryClient } from "@tanstack/react-query";
import { HttpResponse, http } from "msw";
import { afterEach, describe, expect, it } from "vitest";

import {
  clearCurrentSession,
  clearSessionDependentQueries,
  currentSessionQueryKey,
  currentSessionQueryOptions,
  ensureRestoredSession,
  setCurrentSession,
} from "@/domains/session/api/session-queries";
import { isSessionUnavailableError } from "@/domains/session/lib/session-lifecycle";
import {
  createSessionScenarioHandlers,
  renewedSessionPrincipalResponse,
  sessionPrincipalResponse,
} from "@/mocks/scenarios/session";
import { setTestCookie } from "@/test/cookies";
import { apiMockServer } from "@/test/mocks/server";

const session = {
  authenticationMethod: "password",
  principal: {
    email: "member@example.com",
    emailVerified: true,
    id: "user-1",
  },
} as const;

function createQueryClient() {
  return new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
}

afterEach(() => {
  setTestCookie("cardo.csrf=; Max-Age=0; Path=/");
});

describe("session cache", () => {
  it("clears only session-dependent server state", async () => {
    const queryClient = new QueryClient();
    setCurrentSession(queryClient, session);
    await queryClient.fetchQuery({
      meta: { requiresSession: true },
      queryFn: () => Promise.resolve("protected"),
      queryKey: ["protected"],
    });
    await queryClient.fetchQuery({
      queryFn: () => Promise.resolve("public"),
      queryKey: ["public"],
    });

    clearSessionDependentQueries(queryClient);

    expect(queryClient.getQueryData(["protected"])).toBeUndefined();
    expect(queryClient.getQueryData(["public"])).toBe("public");
    expect(queryClient.getQueryData(currentSessionQueryKey)).toEqual(session);

    clearCurrentSession(queryClient);
    expect(queryClient.getQueryData(currentSessionQueryKey)).toBeUndefined();
  });
});

describe("current-session query", () => {
  it("does not renew inactivity when it refetches", async () => {
    let refreshes = 0;
    const queryClient = createQueryClient();
    apiMockServer.use(
      http.post("/api/v1/identity/sessions/current/refresh", () => {
        refreshes += 1;
        return HttpResponse.json(renewedSessionPrincipalResponse);
      }),
    );

    await queryClient.fetchQuery(currentSessionQueryOptions({ locale: "en" }));
    await queryClient.refetchQueries({ queryKey: currentSessionQueryKey });

    expect(queryClient.getQueryData(currentSessionQueryKey)).toMatchObject({
      principal: { email: "member@example.com" },
    });
    expect(refreshes).toBe(0);
  });
});

describe("session restoration", () => {
  it("restores an expired authorization credential with one intentional refresh", async () => {
    setTestCookie("cardo.csrf=mock-csrf-token; Path=/");
    const queryClient = createQueryClient();
    apiMockServer.use(
      ...createSessionScenarioHandlers({ initialSession: "expired" }),
    );

    const restored = await ensureRestoredSession(queryClient, { locale: "en" });

    expect(restored.browserSession?.idleExpiresAt).toBe(
      renewedSessionPrincipalResponse.browserSession.idleExpiresAt,
    );
    expect(queryClient.getQueryData(currentSessionQueryKey)).toEqual(restored);
  });

  it("refreshes a cached session whose authorization credential expired while stale", async () => {
    let refreshes = 0;
    setTestCookie("cardo.csrf=mock-csrf-token; Path=/");
    const queryClient = createQueryClient();
    apiMockServer.use(
      // The counting handler must precede the scenario's own so it is the one that matches.
      http.post("/api/v1/identity/sessions/current/refresh", () => {
        refreshes += 1;
        return HttpResponse.json(renewedSessionPrincipalResponse);
      }),
      ...createSessionScenarioHandlers({ initialSession: "expired" }),
    );
    // A session cached longer ago than staleTime must not be accepted on its age alone.
    setCurrentSession(queryClient, session);
    queryClient
      .getQueryCache()
      .find({ queryKey: currentSessionQueryKey })
      ?.setState({ dataUpdatedAt: Date.now() - 120_000 });

    const restored = await ensureRestoredSession(queryClient, { locale: "en" });

    expect(refreshes).toBe(1);
    expect(restored.browserSession?.idleExpiresAt).toBe(
      renewedSessionPrincipalResponse.browserSession.idleExpiresAt,
    );
  });

  it("reuses a cached session without any request", async () => {
    let reads = 0;
    const queryClient = createQueryClient();
    setCurrentSession(queryClient, session);
    apiMockServer.use(
      http.get("/api/v1/identity/sessions/current", () => {
        reads += 1;
        return HttpResponse.json(sessionPrincipalResponse);
      }),
    );

    await ensureRestoredSession(queryClient, { locale: "en" });

    expect(reads).toBe(0);
  });

  it.each([
    "absolute-expired",
    "idle-expired",
    "revoked",
  ] as const)("ends a %s session without attempting a refresh", async (initialSession) => {
    let refreshes = 0;
    setTestCookie("cardo.csrf=mock-csrf-token; Path=/");
    const queryClient = createQueryClient();
    apiMockServer.use(
      // The counting handler must precede the scenario's own so it is the one that matches.
      http.post("/api/v1/identity/sessions/current/refresh", () => {
        refreshes += 1;
        return HttpResponse.json(renewedSessionPrincipalResponse);
      }),
      ...createSessionScenarioHandlers({ initialSession }),
    );

    await expect(
      ensureRestoredSession(queryClient, { locale: "en" }),
    ).rejects.toSatisfy(isSessionUnavailableError);
    expect(refreshes).toBe(0);
  });

  it("converges when another tab already owns the refresh", async () => {
    setTestCookie("cardo.csrf=mock-csrf-token; Path=/");
    const queryClient = createQueryClient();
    apiMockServer.use(
      ...createSessionScenarioHandlers({ initialSession: "refresh-conflict" }),
    );
    // The competing tab holds the lease while this tab's authorization credential has expired.
    apiMockServer.use(
      http.get(
        "/api/v1/identity/sessions/current",
        () => HttpResponse.json({}, { status: 401 }),
        { once: true },
      ),
    );

    const restored = await ensureRestoredSession(queryClient, { locale: "en" });

    expect(restored.principal.email).toBe("member@example.com");
    expect(queryClient.getQueryData(currentSessionQueryKey)).toEqual(restored);
  });
});
