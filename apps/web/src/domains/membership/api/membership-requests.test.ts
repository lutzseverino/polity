import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";

import { listMembershipInvitations } from "@/domains/membership/api/membership-requests";
import { apiMockServer } from "@/test/mocks/server";

describe("membership invitation requests", () => {
  it("loads the authenticated user's pending invitations from Polity", async () => {
    let acceptedLanguage: string | null = null;
    let requestedSize: string | null = null;
    apiMockServer.use(
      http.get("/api/v1/invitations", ({ request }) => {
        const url = new URL(request.url);
        acceptedLanguage = request.headers.get("Accept-Language");
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
});
