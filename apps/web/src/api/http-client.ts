import axios, {
  AxiosHeaders,
  type AxiosHeaderValue,
  type AxiosInstance,
  type AxiosRequestConfig,
} from "axios";

type HttpClientOptions = Readonly<{
  axiosConfig?: AxiosRequestConfig;
  baseUrl?: string;
}>;

type HttpRequestConfig<RequestData = unknown> = Omit<
  AxiosRequestConfig<RequestData>,
  "baseURL" | "headers"
> &
  Readonly<{
    acceptedLanguage: string;
    headers?: AxiosHeaders | Record<string, AxiosHeaderValue>;
    notifyOnUnauthorized?: boolean;
  }>;

export type HttpClient = Readonly<{
  request<ResponseData, RequestData = unknown>(
    config: HttpRequestConfig<RequestData>,
  ): Promise<ResponseData>;
}>;

class HttpSessionError extends Error {
  readonly kind: "forbidden" | "unauthorized";
  readonly status: 401 | 403;

  constructor(status: 401 | 403) {
    super(
      status === 401
        ? "The session is not authenticated."
        : "The request was rejected.",
    );
    this.name = "HttpSessionError";
    this.kind = status === 401 ? "unauthorized" : "forbidden";
    this.status = status;
  }
}

type UnauthorizedListener = () => void;

let unauthorizedListener: UnauthorizedListener | undefined;
const unsafeMethods = new Set(["DELETE", "PATCH", "POST", "PUT"]);
const csrfCookieNames = ["__Host-cardo.csrf", "cardo.csrf"] as const;

export function setTerminalUnauthorizedHandler(listener: UnauthorizedListener) {
  unauthorizedListener = listener;
  return () => {
    if (unauthorizedListener === listener) unauthorizedListener = undefined;
  };
}

function readCookie(name: string) {
  if (typeof document === "undefined") return undefined;

  const prefix = `${encodeURIComponent(name)}=`;
  const cookie = document.cookie
    .split(";")
    .map((part) => part.trim())
    .find((part) => part.startsWith(prefix));

  return cookie ? decodeURIComponent(cookie.slice(prefix.length)) : undefined;
}

function readCsrfToken() {
  for (const cookieName of csrfCookieNames) {
    const token = readCookie(cookieName);
    if (token) return token;
  }
  return undefined;
}

function notifyTerminalUnauthorized() {
  unauthorizedListener?.();
}

export function createHttpClient({
  axiosConfig,
  baseUrl = "/api/v1",
}: HttpClientOptions = {}): HttpClient {
  const client: AxiosInstance = axios.create({
    ...axiosConfig,
    baseURL: baseUrl,
  });

  return {
    async request<ResponseData, RequestData = unknown>({
      acceptedLanguage,
      headers,
      notifyOnUnauthorized = true,
      ...config
    }: HttpRequestConfig<RequestData>) {
      const requestHeaders = AxiosHeaders.from(headers);
      requestHeaders.set("Accept-Language", acceptedLanguage);

      const method = config.method?.toUpperCase() ?? "GET";
      const csrfToken = unsafeMethods.has(method) ? readCsrfToken() : undefined;
      if (
        csrfToken &&
        !requestHeaders.has("Authorization") &&
        !requestHeaders.has("X-CSRF-TOKEN")
      ) {
        requestHeaders.set("X-CSRF-TOKEN", csrfToken);
      }

      try {
        const response = await client.request<ResponseData>({
          ...config,
          headers: requestHeaders,
        });

        return response.data;
      } catch (error) {
        if (axios.isAxiosError(error)) {
          const status = error.response?.status;
          if (status === 401 || status === 403) {
            if (status === 401 && notifyOnUnauthorized) {
              notifyTerminalUnauthorized();
            }
            throw new HttpSessionError(status);
          }
        }
        throw error;
      }
    },
  };
}

export function hasHttpResponseStatus(error: unknown, status: number) {
  return (
    (error instanceof HttpSessionError && error.status === status) ||
    (axios.isAxiosError(error) && error.response?.status === status)
  );
}

export function hasHttpResponseCode(error: unknown, code: string) {
  if (!axios.isAxiosError(error)) return false;
  const data: unknown = error.response?.data;
  if (!data || typeof data !== "object") return false;
  const body = data as { error?: unknown };
  if (!body.error || typeof body.error !== "object") return false;
  return (body.error as { code?: unknown }).code === code;
}
