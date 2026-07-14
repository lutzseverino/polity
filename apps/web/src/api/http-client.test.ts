import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";

import { createHttpClient } from "@/api/http-client";
import { apiMockServer } from "@/test/mocks/server";

const serviceBaseUrl = "https://api.polity.test/api/v1";

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
});
