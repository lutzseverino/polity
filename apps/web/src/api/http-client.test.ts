import { HttpResponse, http } from "msw";
import { afterEach, describe, expect, it, vi } from "vitest";

import {
  createHttpClient,
  setTerminalUnauthorizedHandler,
} from "@/api/http-client";
import { apiMockServer } from "@/test/mocks/server";

const serviceBaseUrl = "https://api.polity.test/api/v1";

afterEach(() => {
  vi.restoreAllMocks();
});

function mockDocumentCookie(value: string) {
  vi.spyOn(Document.prototype, "cookie", "get").mockReturnValue(value);
}

describe("HTTP client", () => {
  it("sends the current accepted language and returns response data", async () => {
    let acceptedLanguage: string | null = null;
    apiMockServer.use(
      http.get(`${serviceBaseUrl}/polities/polity-1`, ({ request }) => {
        acceptedLanguage = request.headers.get("Accept-Language");

        return HttpResponse.json({ id: "polity-1" });
      }),
    );
    const client = createHttpClient({ baseUrl: serviceBaseUrl });

    const result = await client.request<{ id: string }>({
      acceptedLanguage: "es",
      method: "GET",
      url: "/polities/polity-1",
    });

    expect(acceptedLanguage).toBe("es");
    expect(result).toEqual({ id: "polity-1" });
  });

  it("lets an individual request override ordinary instance headers", async () => {
    let requestSource: string | null = null;
    apiMockServer.use(
      http.post(`${serviceBaseUrl}/polity/account`, ({ request }) => {
        requestSource = request.headers.get("X-Request-Source");

        return new HttpResponse(null, { status: 204 });
      }),
    );
    const client = createHttpClient({
      baseUrl: serviceBaseUrl,
      axiosConfig: {
        headers: { "X-Request-Source": "instance" },
      },
    });

    await client.request({
      acceptedLanguage: "en",
      headers: { "X-Request-Source": "request" },
      method: "POST",
      url: "/polity/account",
    });

    expect(requestSource).toBe("request");
  });

  it.each([
    "DELETE",
    "PATCH",
    "POST",
    "PUT",
  ])("echoes the local CSRF cookie for unsafe %s requests", async (method) => {
    let csrfHeader: string | null = null;
    mockDocumentCookie("cardo.csrf=local-token");
    apiMockServer.use(
      http.all(`${serviceBaseUrl}/resource`, ({ request }) => {
        csrfHeader = request.headers.get("X-CSRF-TOKEN");
        return new HttpResponse(null, { status: 204 });
      }),
    );

    await createHttpClient({ baseUrl: serviceBaseUrl }).request({
      acceptedLanguage: "en",
      method,
      url: "/resource",
    });

    expect(csrfHeader).toBe("local-token");
  });

  it("supports the production CSRF cookie name", async () => {
    let csrfHeader: string | null = null;
    mockDocumentCookie("__Host-cardo.csrf=production-token");
    apiMockServer.use(
      http.post(`${serviceBaseUrl}/resource`, ({ request }) => {
        csrfHeader = request.headers.get("X-CSRF-TOKEN");
        return new HttpResponse(null, { status: 204 });
      }),
    );

    await createHttpClient({ baseUrl: serviceBaseUrl }).request({
      acceptedLanguage: "en",
      method: "POST",
      url: "/resource",
    });

    expect(csrfHeader).toBe("production-token");
  });

  it("does not guess a CSRF token or attach one to safe requests", async () => {
    const csrfHeaders: Array<string | null> = [];
    apiMockServer.use(
      http.get(`${serviceBaseUrl}/safe`, ({ request }) => {
        csrfHeaders.push(request.headers.get("X-CSRF-TOKEN"));
        return new HttpResponse(null, { status: 204 });
      }),
      http.post(`${serviceBaseUrl}/unsafe`, ({ request }) => {
        csrfHeaders.push(request.headers.get("X-CSRF-TOKEN"));
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const client = createHttpClient({ baseUrl: serviceBaseUrl });

    await client.request({
      acceptedLanguage: "en",
      method: "GET",
      url: "/safe",
    });
    await client.request({
      acceptedLanguage: "en",
      method: "POST",
      url: "/unsafe",
    });

    expect(csrfHeaders).toEqual([null, null]);
  });

  it("preserves a caller-provided CSRF header", async () => {
    let csrfHeader: string | null = null;
    mockDocumentCookie("cardo.csrf=cookie-token");
    apiMockServer.use(
      http.post(`${serviceBaseUrl}/resource`, ({ request }) => {
        csrfHeader = request.headers.get("X-CSRF-TOKEN");
        return new HttpResponse(null, { status: 204 });
      }),
    );

    await createHttpClient({ baseUrl: serviceBaseUrl }).request({
      acceptedLanguage: "en",
      headers: { "X-CSRF-TOKEN": "caller-token" },
      method: "POST",
      url: "/resource",
    });

    expect(csrfHeader).toBe("caller-token");
  });

  it("does not attach CSRF when a caller explicitly selects bearer authentication", async () => {
    let csrfHeader: string | null = null;
    mockDocumentCookie("cardo.csrf=cookie-token");
    apiMockServer.use(
      http.post(`${serviceBaseUrl}/resource`, ({ request }) => {
        csrfHeader = request.headers.get("X-CSRF-TOKEN");
        return new HttpResponse(null, { status: 204 });
      }),
    );

    await createHttpClient({ baseUrl: serviceBaseUrl }).request({
      acceptedLanguage: "en",
      headers: { Authorization: "Bearer caller-token" },
      method: "POST",
      url: "/resource",
    });

    expect(csrfHeader).toBeNull();
  });

  it.each([
    401, 403,
  ] as const)("normalizes %s responses without exposing Axios", async (status) => {
    let unauthorizedCount = 0;
    setTerminalUnauthorizedHandler(() => {
      unauthorizedCount += 1;
    });
    apiMockServer.use(
      http.get(`${serviceBaseUrl}/session`, () =>
        HttpResponse.json({ error: { code: "failure" } }, { status }),
      ),
    );

    const request = createHttpClient({ baseUrl: serviceBaseUrl }).request({
      acceptedLanguage: "en",
      method: "GET",
      url: "/session",
    });

    await expect(request).rejects.toMatchObject({ status });
    expect(unauthorizedCount).toBe(status === 401 ? 1 : 0);
  });
});
