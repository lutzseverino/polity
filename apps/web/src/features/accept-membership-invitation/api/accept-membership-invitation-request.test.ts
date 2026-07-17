import { HttpResponse, http } from "msw";
import { describe, expect, it } from "vitest";

import { acceptMembershipInvitation } from "@/features/accept-membership-invitation/api/accept-membership-invitation-request";
import { apiMockServer } from "@/test/mocks/server";

describe("accept membership invitation request", () => {
  it("posts the authenticated acceptance to Polity", async () => {
    let acceptedLanguage: string | null = null;
    apiMockServer.use(
      http.post(
        "/api/v1/invitations/:invitationId/accept",
        ({ params, request }) => {
          expect(params.invitationId).toBe(
            "11111111-1111-1111-1111-111111111111",
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
      invitationId: "11111111-1111-1111-1111-111111111111",
    });

    expect(acceptedLanguage).toBe("es");
    expect(membership).toEqual({ id: "membership-1", status: "active" });
  });
});
