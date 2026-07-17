import { HttpResponse, http } from "msw";
import { setupServer } from "msw/node";

export const apiMockServer = setupServer(
  http.get("/api/v1/invitation-tokens/:token", () =>
    HttpResponse.json({
      expiresAt: "2026-07-20T10:00:00Z",
      invitedEmail: "friend@example.com",
      polityId: "sunday-supper-club",
      polityName: "Sunday Supper Club",
    }),
  ),
);
