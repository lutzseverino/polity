export type AuthenticatedPrincipal = Readonly<{
  avatarUrl?: string;
  email: string;
  emailVerified: boolean;
  id: string;
  name?: string;
}>;

/**
 * Cardo's authoritative browser-session timing. Absent for an explicit bearer response and for
 * Identity versions that predate the contract, so consumers must treat it as optional.
 */
export type BrowserSessionTiming = Readonly<{
  absoluteExpiresAt: string;
  idleExpiresAt: string;
  refreshable: boolean;
  serverTime: string;
  sessionStartedAt: string;
}>;

export type Session = Readonly<{
  authenticationMethod: "oidc" | "password" | "saml";
  browserSession?: BrowserSessionTiming;
  /** Expiry of the Identity authorization credential, not of the browser session. */
  expiresAt?: string;
  principal: AuthenticatedPrincipal;
  sessionId?: string;
}>;

type UnknownRecord = Record<string, unknown>;

function readRecord(value: unknown, field: string): UnknownRecord {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    throw new Error(`Identity session response ${field} is invalid.`);
  }
  return value as UnknownRecord;
}

function readString(value: unknown, field: string) {
  if (typeof value !== "string" || !value) {
    throw new Error(`Identity session response ${field} is invalid.`);
  }
  return value;
}

function readOptionalString(value: unknown, field: string) {
  return value === null || value === undefined
    ? undefined
    : readString(value, field);
}

function readBoolean(value: unknown, field: string) {
  if (typeof value !== "boolean") {
    throw new Error(`Identity session response ${field} is invalid.`);
  }
  return value;
}

function readInstant(value: unknown, field: string) {
  const instant = readString(value, field);
  if (Number.isNaN(Date.parse(instant))) {
    throw new Error(`Identity session response ${field} is invalid.`);
  }
  return instant;
}

function readBrowserSession(value: unknown): BrowserSessionTiming | undefined {
  if (value === null || value === undefined) return undefined;

  const timing = readRecord(value, "browserSession");
  return {
    absoluteExpiresAt: readInstant(
      timing.absoluteExpiresAt,
      "browserSession.absoluteExpiresAt",
    ),
    idleExpiresAt: readInstant(
      timing.idleExpiresAt,
      "browserSession.idleExpiresAt",
    ),
    refreshable: readBoolean(timing.refreshable, "browserSession.refreshable"),
    serverTime: readInstant(timing.serverTime, "browserSession.serverTime"),
    sessionStartedAt: readInstant(
      timing.sessionStartedAt,
      "browserSession.sessionStartedAt",
    ),
  };
}

export function parseSession(value: unknown): Session {
  const response = readRecord(value, "body");
  const user = readRecord(response.user, "user");
  const authenticationMethod = readString(
    response.authenticationMethod,
    "authenticationMethod",
  );

  if (
    !(["oidc", "password", "saml"] as const).includes(
      authenticationMethod as "oidc" | "password" | "saml",
    )
  ) {
    throw new Error(
      "Identity session response authenticationMethod is invalid.",
    );
  }

  return {
    authenticationMethod:
      authenticationMethod as Session["authenticationMethod"],
    browserSession: readBrowserSession(response.browserSession),
    expiresAt: readOptionalString(response.expiresAt, "expiresAt"),
    principal: {
      avatarUrl: readOptionalString(user.avatarUrl, "user.avatarUrl"),
      email: readString(user.email, "user.email"),
      emailVerified: readBoolean(user.emailVerified, "user.emailVerified"),
      id: readString(user.id, "user.id"),
      name: readOptionalString(user.name, "user.name"),
    },
    sessionId: readOptionalString(response.sessionId, "sessionId"),
  };
}
