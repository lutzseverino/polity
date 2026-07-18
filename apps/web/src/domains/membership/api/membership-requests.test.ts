import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";

import {
  getMembershipInvitation,
  getMembershipInvitationByToken,
  listMembershipInvitations,
  requestMembershipInvitationCompletion,
} from "@/domains/membership/api/membership-requests";
import { apiMockServer } from "@/test/mocks/server";

describe("membership invitation requests", () => {
  it("rejects a malformed invitation id before loading invitation pages", async () => {
    let requestCount = 0;
    apiMockServer.use(
      http.get("/api/v1/invitations", () => {
        requestCount += 1;
        return HttpResponse.json({
          content: [],
          page: { number: 0, size: 100, totalElements: 0, totalPages: 0 },
        });
      }),
    );

    await expect(
      getMembershipInvitation("not-a-uuid", { acceptedLanguage: "en" }),
    ).rejects.toThrow("Membership invitation not-a-uuid was not found.");
    expect(requestCount).toBe(0);
  });

  it("loads the authenticated user's pending invitations from Polity", async () => {
    let acceptedLanguage: string | null = null;
    let requestedPage: string | null = null;
    let requestedSize: string | null = null;
    apiMockServer.use(
      http.get("/api/v1/invitations", ({ request }) => {
        const url = new URL(request.url);
        acceptedLanguage = request.headers.get("Accept-Language");
        requestedPage = url.searchParams.get("page");
        requestedSize = url.searchParams.get("size");
        return HttpResponse.json({
          content: [
            {
              email: "friend@example.com",
              id: "91111111-1111-4111-8111-111111111111",
              invitedAt: "2026-07-17T12:00:00Z",
              invitedByName: "Mira Chen",
              polityId: "51111111-1111-4111-8111-111111111111",
              polityName: "Garden Cooperative",
              status: "pending",
            },
          ],
          page: { number: 0, size: 100, totalElements: 1, totalPages: 1 },
        });
      }),
    );

    const invitations = await listMembershipInvitations({
      acceptedLanguage: "en",
    });

    expect(acceptedLanguage).toBe("en");
    expect(requestedPage).toBe("0");
    expect(requestedSize).toBe("100");
    expect(invitations).toEqual([
      {
        id: "91111111-1111-4111-8111-111111111111",
        invitedAtLabel: "Jul 17, 2026",
        invitedByName: "Mira Chen",
        polityName: "Garden Cooperative",
      },
    ]);
  });

  it("searches subsequent pages for a directly opened invitation", async () => {
    const requestedPages: string[] = [];
    apiMockServer.use(
      http.get("/api/v1/invitations", ({ request }) => {
        const page = new URL(request.url).searchParams.get("page") ?? "0";
        requestedPages.push(page);
        return HttpResponse.json({
          content:
            page === "1"
              ? [
                  {
                    email: "friend@example.com",
                    id: "91111111-1111-4111-8111-111111111115",
                    invitedAt: "2026-07-17T12:00:00Z",
                    invitedByName: "Mira Chen",
                    polityId: "51111111-1111-4111-8111-111111111111",
                    polityName: "Garden Cooperative",
                    status: "pending",
                  },
                ]
              : [],
          page: {
            number: Number(page),
            size: 100,
            totalElements: 101,
            totalPages: 2,
          },
        });
      }),
    );

    const invitation = await getMembershipInvitation(
      "91111111-1111-4111-8111-111111111115",
      {
        acceptedLanguage: "en",
      },
    );

    expect(requestedPages).toEqual(["0", "1"]);
    expect(invitation.id).toBe("91111111-1111-4111-8111-111111111115");
  });

  it("rejects a successful response with an invalid transport shape", async () => {
    apiMockServer.use(
      http.get("/api/v1/invitations", () =>
        HttpResponse.text("<!doctype html><title>Vite</title>"),
      ),
    );

    await expect(
      listMembershipInvitations({ acceptedLanguage: "en" }),
    ).rejects.toThrow("Invalid membership invitation response.");
  });

  it("maps remote completion details to product-owned state", async () => {
    apiMockServer.use(
      http.post("/api/v1/invitation-tokens/:token/completion", () =>
        HttpResponse.json(
          {
            actionExpiresAt: "2026-07-18T10:16:00Z",
            attemptCount: 3,
            createdAt: "2026-07-18T10:00:00Z",
            lastError: "credential_action_expired",
            status: "failed",
            updatedAt: "2026-07-18T10:01:00Z",
          },
          { status: 202 },
        ),
      ),
    );

    const completion =
      await requestMembershipInvitationCompletion("invitation-failed");

    expect(completion).toEqual({ status: "failed" });
    expect(completion).not.toHaveProperty("lastError");
    expect(completion).not.toHaveProperty("actionExpiresAt");
  });

  it("rejects an unknown completion state instead of returning to signup", async () => {
    apiMockServer.use(
      http.post("/api/v1/invitation-tokens/:token/completion", () =>
        HttpResponse.json(
          {
            attemptCount: 1,
            createdAt: "2026-07-18T10:00:00Z",
            status: "unknown",
            updatedAt: "2026-07-18T10:01:00Z",
          },
          { status: 202 },
        ),
      ),
    );

    await expect(
      requestMembershipInvitationCompletion("invitation-malformed"),
    ).rejects.toThrow("Invalid membership invitation completion response.");
  });

  it("maps token transport dates to localized product copy", async () => {
    let acceptedLanguage: string | null = null;
    apiMockServer.use(
      http.get("/api/v1/invitation-tokens/:token", ({ request }) => {
        acceptedLanguage = request.headers.get("Accept-Language");
        return HttpResponse.json({
          expiresAt: "2026-07-20T10:00:00Z",
          invitedEmail: "friend@example.com",
          polityId: "51111111-1111-4111-8111-111111111111",
          polityName: "Garden Cooperative",
        });
      }),
    );

    const invitation = await getMembershipInvitationByToken(
      "invitation-token",
      { acceptedLanguage: "en" },
    );

    expect(acceptedLanguage).toBe("en");
    expect(invitation).toEqual({
      expiresAtLabel: "July 20, 2026",
      invitedEmail: "friend@example.com",
      polityId: "51111111-1111-4111-8111-111111111111",
      polityName: "Garden Cooperative",
    });
  });

  it("rejects malformed invitation token context", async () => {
    apiMockServer.use(
      http.get("/api/v1/invitation-tokens/:token", () =>
        HttpResponse.json({
          expiresAt: "not-a-date",
          invitedEmail: "friend@example.com",
          polityId: "51111111-1111-4111-8111-111111111111",
          polityName: "Garden Cooperative",
        }),
      ),
    );

    await expect(
      getMembershipInvitationByToken("invitation-malformed", {
        acceptedLanguage: "en",
      }),
    ).rejects.toThrow("Invalid membership invitation token response.");
  });
});
