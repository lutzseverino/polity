import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";

import { acceptMembershipInvitation } from "@/features/accept-membership-invitation/api/accept-membership-invitation-request";
import { apiMockServer } from "@/test/mocks/server";

describe("accept membership invitation request", () => {
  it("rejects a malformed invitation id before posting acceptance", async () => {
    let requestCount = 0;
    apiMockServer.use(
      http.post("/api/v1/invitations/:invitationId/accept", () => {
        requestCount += 1;
        return HttpResponse.json({ id: "membership-1", status: "active" });
      }),
    );

    expect(() =>
      acceptMembershipInvitation({
        acceptedLanguage: "en",
        invitationId: "not-a-uuid",
      }),
    ).toThrow("Membership invitation not-a-uuid was not found.");
    expect(requestCount).toBe(0);
  });

  it("posts the authenticated acceptance to Polity", async () => {
    let acceptedLanguage: string | null = null;
    apiMockServer.use(
      http.post(
        "/api/v1/invitations/:invitationId/accept",
        ({ params, request }) => {
          expect(params.invitationId).toBe(
            "11111111-1111-4111-8111-111111111111",
          );
          acceptedLanguage = request.headers.get("Accept-Language");
          return HttpResponse.json(
            { id: "membership-1", status: "active" },
            { status: 201 },
          );
        },
      ),
    );

    const membership = await acceptMembershipInvitation({
      acceptedLanguage: "es",
      invitationId: "11111111-1111-4111-8111-111111111111",
    });

    expect(acceptedLanguage).toBe("es");
    expect(membership).toEqual({ id: "membership-1", status: "active" });
  });
});
