import { createHttpClient, hasHttpResponseStatus } from "@/api/http-client";

class SignOutError extends Error {
  constructor() {
    super("Sign out could not be completed.");
    this.name = "SignOutError";
  }
}

const httpClient = createHttpClient();

export async function signOut(acceptedLanguage: string) {
  try {
    await httpClient.request<void>({
      acceptedLanguage,
      method: "DELETE",
      notifyOnUnauthorized: false,
      url: "/identity/sessions/current",
    });
  } catch (error) {
    if (hasHttpResponseStatus(error, 401)) return;
    throw new SignOutError();
  }
}
