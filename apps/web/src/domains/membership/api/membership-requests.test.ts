import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";

import {
  getMembershipInvitation,
  listMembershipInvitations,
} from "@/domains/membership/api/membership-requests";
import { apiMockServer } from "@/test/mocks/server";

describe("membership invitation requests", () => {
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
              id: "invitation-1",
              invitedAt: "2026-07-17T12:00:00Z",
              invitedByName: "Mira Chen",
              polityId: "polity-1",
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
        id: "invitation-1",
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
                    id: "invitation-101",
                    invitedAt: "2026-07-17T12:00:00Z",
                    invitedByName: "Mira Chen",
                    polityId: "polity-1",
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

    const invitation = await getMembershipInvitation("invitation-101", {
      acceptedLanguage: "en",
    });

    expect(requestedPages).toEqual(["0", "1"]);
    expect(invitation.id).toBe("invitation-101");
  });
});
