export type AuthenticatedPrincipal = Readonly<{
  avatarUrl?: string;
  email: string;
  emailVerified: boolean;
  id: string;
  name?: string;
}>;

export type Session = Readonly<{
  authenticationMethod: "oidc" | "password" | "saml";
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
