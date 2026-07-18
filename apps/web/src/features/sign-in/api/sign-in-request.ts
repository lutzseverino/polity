import {
  createHttpClient,
  hasHttpResponseCode,
  hasHttpResponseStatus,
} from "@/api/http-client";
import {
  bootstrapSessionCsrf,
  parseSession,
  type Session,
} from "@/domains/session";

export type SignInInput = Readonly<{
  email: string;
  password: string;
}>;

type SignInRequest = SignInInput &
  Readonly<{
    acceptedLanguage: string;
  }>;

export class SignInError extends Error {
  readonly kind: "invalid-credentials" | "rejected" | "unavailable";

  constructor(kind: SignInError["kind"]) {
    super("Sign in could not be completed.");
    this.name = "SignInError";
    this.kind = kind;
  }
}

const httpClient = createHttpClient();

export async function signIn({
  acceptedLanguage,
  email,
  password,
}: SignInRequest): Promise<Session> {
  try {
    await bootstrapSessionCsrf({ acceptedLanguage });
    const response = await httpClient.request<unknown, SignInInput>({
      acceptedLanguage,
      data: { email, password },
      method: "POST",
      notifyOnUnauthorized: false,
      url: "/identity/sessions",
    });
    return parseSession(response);
  } catch (error) {
    if (
      hasHttpResponseStatus(error, 400) &&
      hasHttpResponseCode(error, "invalid_credentials")
    ) {
      throw new SignInError("invalid-credentials");
    }
    if (hasHttpResponseStatus(error, 403)) {
      throw new SignInError("rejected");
    }
    if (error instanceof SignInError) throw error;
    throw new SignInError("unavailable");
  }
}
