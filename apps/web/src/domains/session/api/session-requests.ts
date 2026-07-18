import { createHttpClient, hasHttpResponseStatus } from "@/api/http-client";
import { parseSession, type Session } from "@/domains/session/lib/session";

type SessionRequest = Readonly<{
  acceptedLanguage: string;
  signal?: AbortSignal;
}>;

const httpClient = createHttpClient();
let refreshInFlight: Promise<Session> | undefined;

export class SessionUnavailableError extends Error {
  constructor() {
    super("No restorable session is available.");
    this.name = "SessionUnavailableError";
  }
}

export function bootstrapSessionCsrf({
  acceptedLanguage,
  signal,
}: SessionRequest) {
  return httpClient.request<void>({
    acceptedLanguage,
    method: "GET",
    notifyOnUnauthorized: false,
    signal,
    url: "/identity/sessions/csrf",
  });
}

export async function getCurrentSession({
  acceptedLanguage,
  signal,
}: SessionRequest) {
  const response = await httpClient.request<unknown>({
    acceptedLanguage,
    method: "GET",
    notifyOnUnauthorized: false,
    signal,
    url: "/identity/sessions/current",
  });
  return parseSession(response);
}

export async function refreshCurrentSession({
  acceptedLanguage,
  signal,
}: SessionRequest) {
  const response = await httpClient.request<unknown>({
    acceptedLanguage,
    method: "POST",
    notifyOnUnauthorized: false,
    signal,
    url: "/identity/sessions/current/refresh",
  });
  return parseSession(response);
}

function coordinateRefresh(request: SessionRequest) {
  refreshInFlight ??= refreshCurrentSession({
    acceptedLanguage: request.acceptedLanguage,
  }).finally(() => {
    refreshInFlight = undefined;
  });
  return refreshInFlight;
}

export async function restoreCurrentSession(request: SessionRequest) {
  try {
    return await getCurrentSession(request);
  } catch (error) {
    if (hasHttpResponseStatus(error, 403)) {
      throw new SessionUnavailableError();
    }
    if (!hasHttpResponseStatus(error, 401)) throw error;
  }

  try {
    await coordinateRefresh(request);
    return await getCurrentSession(request);
  } catch (error) {
    if (
      hasHttpResponseStatus(error, 401) ||
      hasHttpResponseStatus(error, 403)
    ) {
      throw new SessionUnavailableError();
    }
    throw error;
  }
}

export function isSessionUnavailableError(
  error: unknown,
): error is SessionUnavailableError {
  return error instanceof SessionUnavailableError;
}
