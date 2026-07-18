import { HttpResponse, http, type RequestHandler } from "msw";

type SessionScenarioOptions = Readonly<{
  initialSession?: "expired" | "revoked" | "signed-in" | "signed-out";
}>;

export const sessionPrincipalResponse = {
  authProviderId: null,
  authenticationMethod: "password",
  expiresAt: "2026-07-18T12:00:00.000Z",
  grants: [],
  sessionId: "mock-session",
  user: {
    authorizationSubject: "mock-identity-subject",
    avatarUrl: null,
    createdAt: "2026-01-01T00:00:00.000Z",
    email: "member@example.com",
    emailVerified: true,
    id: "99999999-9999-4999-8999-999999999999",
    name: "Mira Chen",
    status: "active",
    updatedAt: "2026-01-01T00:00:00.000Z",
  },
} as const;

const csrfToken = "mock-csrf-token";

export function materializeBrowserSessionScenarioCsrf() {
  // biome-ignore lint/suspicious/noDocumentCookie: the development scenario must emulate Cardo's Set-Cookie response in the page.
  document.cookie = `cardo.csrf=${csrfToken}; Path=/; SameSite=Lax`;
}

function readCookie(request: Request, name: string) {
  const prefix = `${name}=`;
  return request.headers
    .get("Cookie")
    ?.split(";")
    .map((part) => part.trim())
    .find((part) => part.startsWith(prefix))
    ?.slice(prefix.length);
}

function hasValidCsrf(request: Request) {
  const cookie =
    readCookie(request, "__Host-cardo.csrf") ??
    readCookie(request, "cardo.csrf");
  const header = request.headers.get("X-CSRF-TOKEN");
  return header === csrfToken && (cookie === undefined || cookie === header);
}

function csrfRejected() {
  return HttpResponse.json(
    { error: { code: "invalid_csrf", message: "CSRF validation failed." } },
    { status: 403 },
  );
}

export function createSessionScenarioHandlers({
  initialSession = "signed-in",
}: SessionScenarioOptions = {}): RequestHandler[] {
  let signedIn = initialSession !== "signed-out";
  let accessValid = initialSession === "signed-in";
  let refreshValid =
    initialSession === "signed-in" || initialSession === "expired";

  return [
    http.get(
      "/api/v1/identity/sessions/csrf",
      () =>
        new HttpResponse(null, {
          headers: {
            "Cache-Control": "no-store",
            "Set-Cookie": `cardo.csrf=${csrfToken}; Path=/; SameSite=Lax`,
          },
          status: 204,
        }),
    ),
    http.post("/api/v1/identity/sessions", async ({ request }) => {
      if (!hasValidCsrf(request)) return csrfRejected();

      const credentials = (await request.json()) as {
        email?: unknown;
        password?: unknown;
      };
      if (
        credentials.email !== "member@example.com" ||
        credentials.password !== "correct-password"
      ) {
        return HttpResponse.json(
          {
            error: {
              code: "invalid_credentials",
              message: "Invalid credentials.",
            },
          },
          { status: 400 },
        );
      }

      signedIn = true;
      accessValid = true;
      refreshValid = true;
      return HttpResponse.json(sessionPrincipalResponse, { status: 201 });
    }),
    http.get("/api/v1/identity/sessions/current", () =>
      signedIn && accessValid
        ? HttpResponse.json(sessionPrincipalResponse)
        : HttpResponse.json(
            {
              error: {
                code: "authentication_required",
                message: "Authentication is required.",
              },
            },
            { status: 401 },
          ),
    ),
    http.post("/api/v1/identity/sessions/current/refresh", ({ request }) => {
      if (!hasValidCsrf(request)) return csrfRejected();
      if (!signedIn || !refreshValid) {
        return HttpResponse.json(
          {
            error: {
              code: "invalid_session",
              message: "The session is no longer valid.",
            },
          },
          { status: 401 },
        );
      }

      accessValid = true;
      return HttpResponse.json(sessionPrincipalResponse);
    }),
    http.delete("/api/v1/identity/sessions/current", ({ request }) => {
      if (!hasValidCsrf(request)) return csrfRejected();
      signedIn = false;
      accessValid = false;
      refreshValid = false;
      return new HttpResponse(null, {
        headers: {
          "Set-Cookie": "cardo.csrf=; Max-Age=0; Path=/; SameSite=Lax",
        },
        status: 204,
      });
    }),
  ];
}
