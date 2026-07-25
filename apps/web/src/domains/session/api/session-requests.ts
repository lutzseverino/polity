import { createHttpClient } from "@/api/http-client";
import { parseSession, type Session } from "@/domains/session/lib/session";
import {
  isRefreshConflict,
  readSessionEndReason,
  type SessionEndReason,
  SessionUnavailableError,
} from "@/domains/session/lib/session-lifecycle";

type SessionRequest = Readonly<{
  acceptedLanguage: string;
  signal?: AbortSignal;
}>;

const httpClient = createHttpClient();
let refreshInFlight: Promise<Session> | undefined;

/**
 * A refresh that another browser tab already owns has not finished rotating credentials yet, so one
 * bounded re-read gives that tab time to publish the new cookies before the session is given up.
 */
const conflictConvergenceAttempts = 2;
const conflictConvergenceDelayMs = 150;

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

/**
 * Reads the current session without ever refreshing. Identity renews the idle deadline only on an
 * explicit refresh, so keeping this read refresh-free is what stops background product traffic and
 * routine session reads from extending inactivity.
 */
export async function readCurrentSession(request: SessionRequest) {
  try {
    return await getCurrentSession(request);
  } catch (error) {
    const reason = readSessionEndReason(error);
    if (!reason) throw error;
    throw new SessionUnavailableError(reason);
  }
}

function coordinateRefresh(request: SessionRequest) {
  refreshInFlight ??= refreshCurrentSession({
    acceptedLanguage: request.acceptedLanguage,
  }).finally(() => {
    refreshInFlight = undefined;
  });
  return refreshInFlight;
}

function delay(milliseconds: number) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

async function convergeOnConcurrentRefresh(request: SessionRequest) {
  let lastReason: SessionEndReason = "unauthenticated";

  for (let attempt = 0; attempt < conflictConvergenceAttempts; attempt += 1) {
    if (attempt > 0) await delay(conflictConvergenceDelayMs);
    try {
      return await getCurrentSession(request);
    } catch (error) {
      const reason = readSessionEndReason(error);
      if (!reason) throw error;
      lastReason = reason;
    }
  }

  throw new SessionUnavailableError(lastReason);
}

/**
 * The intentional refresh. This is the only operation that renews Cardo's idle deadline, so it
 * belongs to session restoration and is never reachable from an ordinary read. Its response carries
 * the authoritative principal and browser-session timing, so no confirming read follows it.
 */
export async function renewCurrentSession(request: SessionRequest) {
  try {
    return await coordinateRefresh(request);
  } catch (error) {
    if (isRefreshConflict(error)) {
      return await convergeOnConcurrentRefresh(request);
    }
    const reason = readSessionEndReason(error);
    if (!reason) throw error;
    throw new SessionUnavailableError(reason);
  }
}
