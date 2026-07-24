import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";

import {
  getCurrentAccount,
  getOrProvisionCurrentAccount,
  provisionCurrentAccount,
} from "@/domains/account/api/account-requests";
import { apiMockServer } from "@/test/mocks/server";

const appliedAccountResponse = {
  grants: {
    failureCode: null,
    receiptId: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
    status: "applied",
  },
  userId: "99999999-9999-4999-8999-999999999999",
} as const;

describe("Polity account requests", () => {
  it("reads an existing account without provisioning it again", async () => {
    let provisions = 0;
    apiMockServer.use(
      http.get("/api/v1/polity/account", () =>
        HttpResponse.json(appliedAccountResponse),
      ),
      http.post("/api/v1/polity/account", () => {
        provisions += 1;
        return HttpResponse.json(appliedAccountResponse);
      }),
    );

    const account = await getOrProvisionCurrentAccount({
      acceptedLanguage: "es",
    });

    expect(account.grants.status).toBe("applied");
    expect(provisions).toBe(0);
  });

  it("provisions exactly once when the account does not exist", async () => {
    let acceptedLanguage: string | null = null;
    let provisions = 0;
    apiMockServer.use(
      http.get("/api/v1/polity/account", () =>
        HttpResponse.json({}, { status: 404 }),
      ),
      http.post("/api/v1/polity/account", ({ request }) => {
        acceptedLanguage = request.headers.get("Accept-Language");
        provisions += 1;
        return HttpResponse.json(
          {
            ...appliedAccountResponse,
            grants: {
              ...appliedAccountResponse.grants,
              status: "pending",
            },
          },
          { status: 201 },
        );
      }),
    );

    const account = await getOrProvisionCurrentAccount({
      acceptedLanguage: "es",
    });

    expect(account.grants.status).toBe("pending");
    expect(acceptedLanguage).toBe("es");
    expect(provisions).toBe(1);
  });

  it("validates direct reads and provisions through the same projection", async () => {
    apiMockServer.use(
      http.get("/api/v1/polity/account", () =>
        HttpResponse.json({ ...appliedAccountResponse, userId: "invalid" }),
      ),
      http.post("/api/v1/polity/account", () =>
        HttpResponse.json({
          ...appliedAccountResponse,
          grants: {
            ...appliedAccountResponse.grants,
            failureCode: "grant_application_failed",
            status: "failed",
          },
        }),
      ),
    );

    await expect(getCurrentAccount({ acceptedLanguage: "en" })).rejects.toThrow(
      "Invalid Polity account response.",
    );
    await expect(
      provisionCurrentAccount({ acceptedLanguage: "en" }),
    ).resolves.toMatchObject({
      grants: {
        failureCode: "grant_application_failed",
        status: "failed",
      },
    });
  });
});
