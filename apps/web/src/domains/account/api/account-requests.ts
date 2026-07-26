import { createHttpClient, hasHttpResponseStatus } from "@/api/http-client";
import { parsePolityAccount } from "@/domains/account/api/account-contract";

type AccountRequest = Readonly<{
  acceptedLanguage: string;
  signal?: AbortSignal;
}>;

const httpClient = createHttpClient();

export async function getCurrentAccount({
  acceptedLanguage,
  signal,
}: AccountRequest) {
  const response = await httpClient.request<unknown>({
    acceptedLanguage,
    method: "GET",
    signal,
    url: "/polity/account",
  });
  return parsePolityAccount(response);
}

export async function provisionCurrentAccount({
  acceptedLanguage,
  signal,
}: AccountRequest) {
  const response = await httpClient.request<unknown>({
    acceptedLanguage,
    method: "POST",
    signal,
    url: "/polity/account",
  });
  return parsePolityAccount(response);
}

export async function getOrProvisionCurrentAccount(request: AccountRequest) {
  try {
    return await getCurrentAccount(request);
  } catch (error) {
    if (!hasHttpResponseStatus(error, 404)) throw error;
    return provisionCurrentAccount(request);
  }
}
