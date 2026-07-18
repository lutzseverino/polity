import { HttpResponse, http } from "msw";
import { afterEach, describe, expect, it } from "vitest";

import { signIn } from "@/features/sign-in/api/sign-in-request";
import { createSessionScenarioHandlers } from "@/mocks/scenarios/session";
import { setTestCookie } from "@/test/cookies";
import { apiMockServer } from "@/test/mocks/server";

afterEach(() => {
  setTestCookie("cardo.csrf=; Max-Age=0; Path=/");
});

describe("sign in request", () => {
  it("bootstraps CSRF and establishes a Cardo session", async () => {
    setTestCookie("cardo.csrf=mock-csrf-token; Path=/");
    apiMockServer.use(
      ...createSessionScenarioHandlers({ initialSession: "signed-out" }),
    );

    const session = await signIn({
      acceptedLanguage: "en",
      email: "member@example.com",
      password: "correct-password",
    });

    expect(session.principal.email).toBe("member@example.com");
  });

  it("maps invalid credentials to a stable feature error", async () => {
    setTestCookie("cardo.csrf=mock-csrf-token; Path=/");
    apiMockServer.use(
      ...createSessionScenarioHandlers({ initialSession: "signed-out" }),
    );

    const request = signIn({
      acceptedLanguage: "en",
      email: "member@example.com",
      password: "wrong-password",
    });

    await expect(request).rejects.toMatchObject({
      kind: "invalid-credentials",
    });
  });

  it("maps CSRF rejection without exposing the server response", async () => {
    apiMockServer.use(
      http.post("/api/v1/identity/sessions", () =>
        HttpResponse.json({}, { status: 403 }),
      ),
    );

    await expect(
      signIn({
        acceptedLanguage: "en",
        email: "member@example.com",
        password: "correct-password",
      }),
    ).rejects.toMatchObject({ kind: "rejected" });
  });
});
