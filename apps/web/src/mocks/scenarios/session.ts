import { HttpResponse, http, type RequestHandler } from "msw";

type SessionScenarioOptions = Readonly<{
  initialSession?:
    | "absolute-expired"
    | "expired"
    | "idle-expired"
    | "refresh-conflict"
    | "revoked"
    | "signed-in"
    | "signed-out";
}>;

const sessionStartedAt = "2026-07-18T09:00:00.000Z";
const serverTime = "2026-07-18T11:30:00.000Z";
const idleExpiresAt = "2026-07-18T12:00:00.000Z";
const absoluteExpiresAt = "2026-07-18T17:00:00.000Z";
const renewedIdleExpiresAt = "2026-07-18T12:30:00.000Z";

export const browserSessionResponse = {
  absoluteExpiresAt,
  idleExpiresAt,
  refreshable: true,
  serverTime,
  sessionStartedAt,
} as const;

export const sessionPrincipalResponse = {
  authProviderId: null,
  authenticationMethod: "password",
  browserSession: browserSessionResponse,
  expiresAt: "2026-07-18T11:35:00.000Z",
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

/**
 * Only an explicit refresh renews Cardo's idle deadline, so the renewed principal is a distinct
 * response rather than a repeat of the read.
 */
export const renewedSessionPrincipalResponse = {
  ...sessionPrincipalResponse,
  browserSession: {
    ...browserSessionResponse,
    idleExpiresAt: renewedIdleExpiresAt,
  },
  expiresAt: "2026-07-18T12:05:00.000Z",
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

/** Identity answers every session failure with a stable code and `Cache-Control: no-store`. */
function sessionFailure(status: number, code: string, message: string) {
  return HttpResponse.json(
    { error: { code, message } },
    { headers: { "Cache-Control": "no-store" }, status },
  );
}

function sessionPrincipal(
  response:
    | typeof sessionPrincipalResponse
    | typeof renewedSessionPrincipalResponse,
) {
  return HttpResponse.json(response, {
    headers: { "Cache-Control": "no-store" },
  });
}

export function createSessionScenarioHandlers({
  initialSession = "signed-in",
}: SessionScenarioOptions = {}): RequestHandler[] {
  let signedIn = initialSession !== "signed-out";
  let accessValid = initialSession === "signed-in";
  let refreshValid =
    initialSession === "signed-in" ||
    initialSession === "expired" ||
    initialSession === "refresh-conflict";
  /** A lifecycle end that Identity states, which no refresh can recover. */
  let endedReason: string | undefined =
    initialSession === "idle-expired"
      ? "session_idle_expired"
      : initialSession === "absolute-expired"
        ? "session_absolute_expired"
        : initialSession === "revoked"
          ? "session_revoked"
          : undefined;
  /** Emulates a concurrent browser tab that already owns the refresh lease. */
  let refreshHeldByAnotherTab = initialSession === "refresh-conflict";

  function endedFailure() {
    if (endedReason === "session_absolute_expired") {
      return sessionFailure(
        401,
        endedReason,
        "The browser session reached its absolute deadline.",
      );
    }
    if (endedReason === "session_idle_expired") {
      return sessionFailure(
        401,
        endedReason,
        "The browser session expired after inactivity.",
      );
    }
    return sessionFailure(
      401,
      "session_revoked",
      "The browser session was revoked.",
    );
  }

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
      endedReason = undefined;
      refreshHeldByAnotherTab = false;
      return HttpResponse.json(sessionPrincipalResponse, {
        headers: { "Cache-Control": "no-store" },
        status: 201,
      });
    }),
    http.get("/api/v1/identity/sessions/current", () => {
      if (endedReason) return endedFailure();
      if (signedIn && accessValid) {
        return sessionPrincipal(sessionPrincipalResponse);
      }
      return sessionFailure(
        401,
        "authentication_required",
        "Authentication is required.",
      );
    }),
    http.post("/api/v1/identity/sessions/current/refresh", ({ request }) => {
      if (!hasValidCsrf(request)) return csrfRejected();
      if (endedReason) return endedFailure();
      if (refreshHeldByAnotherTab) {
        // The competing tab completes its rotation, so the next read converges on its result.
        refreshHeldByAnotherTab = false;
        accessValid = true;
        return sessionFailure(
          409,
          "session_refresh_in_progress",
          "The browser session is already being refreshed.",
        );
      }
      if (!signedIn || !refreshValid) {
        return sessionFailure(
          401,
          "refresh_credential_invalid",
          "The refresh credential is not valid.",
        );
      }

      accessValid = true;
      return sessionPrincipal(renewedSessionPrincipalResponse);
    }),
    http.delete("/api/v1/identity/sessions/current", ({ request }) => {
      if (!hasValidCsrf(request)) return csrfRejected();
      signedIn = false;
      accessValid = false;
      refreshValid = false;
      endedReason = "session_revoked";
      return new HttpResponse(null, {
        headers: {
          "Set-Cookie": "cardo.csrf=; Max-Age=0; Path=/; SameSite=Lax",
        },
        status: 204,
      });
    }),
  ];
}
