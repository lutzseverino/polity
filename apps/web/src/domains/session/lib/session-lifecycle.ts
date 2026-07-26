import { getHttpResponseCode, getHttpResponseStatus } from "@/api/http-client";

/**
 * Why a browser session stopped being usable. Cardo's stable lifecycle codes are preserved verbatim
 * so the session domain can distinguish idle expiry, absolute expiry, and revocation. `forbidden`
 * and `unauthenticated` are the normalized fallbacks for a rejection that carries no known code,
 * including responses from Identity versions that predate the browser-session contract.
 */
export type SessionEndReason =
  | "forbidden"
  | "refresh_credential_invalid"
  | "refresh_credential_required"
  | "session_absolute_expired"
  | "session_idle_expired"
  | "session_revoked"
  | "unauthenticated";

const lifecycleCodes = new Set<SessionEndReason>([
  "refresh_credential_invalid",
  "refresh_credential_required",
  "session_absolute_expired",
  "session_idle_expired",
  "session_revoked",
]);

/**
 * Cardo answers a losing concurrent refresh with `409` instead of ending the session. Another
 * browser tab owns or already completed that rotation, so the session may still be usable.
 */
const refreshConflictCodes = new Set([
  "session_refresh_in_progress",
  "session_refresh_superseded",
]);

/** A session that ended, carrying Cardo's reason when Identity supplied one. */
export class SessionUnavailableError extends Error {
  readonly reason: SessionEndReason;

  constructor(reason: SessionEndReason) {
    super(`No restorable session is available: ${reason}.`);
    this.name = "SessionUnavailableError";
    this.reason = reason;
  }
}

export function isSessionUnavailableError(
  error: unknown,
): error is SessionUnavailableError {
  return error instanceof SessionUnavailableError;
}

/**
 * Classifies a session rejection. Returns `undefined` for any failure that is not a session
 * rejection, such as a transport or upstream-provider error, so it can propagate unchanged.
 */
export function readSessionEndReason(
  error: unknown,
): SessionEndReason | undefined {
  const status = getHttpResponseStatus(error);
  if (status !== 401 && status !== 403) return undefined;

  const code = getHttpResponseCode(error);
  if (code && lifecycleCodes.has(code as SessionEndReason)) {
    return code as SessionEndReason;
  }
  return status === 403 ? "forbidden" : "unauthenticated";
}

/**
 * Only an expired authorization credential is worth an explicit refresh. Every stated lifecycle
 * reason means Identity has already ended the browser session, so refreshing cannot recover it.
 */
export function isRecoverableSessionEnd(reason: SessionEndReason) {
  return reason === "unauthenticated";
}

export function isRefreshConflict(error: unknown) {
  if (getHttpResponseStatus(error) !== 409) return false;
  const code = getHttpResponseCode(error);
  return code !== undefined && refreshConflictCodes.has(code);
}
