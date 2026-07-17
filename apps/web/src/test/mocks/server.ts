import { HttpResponse, http } from "msw";
import { setupServer } from "msw/node";

export const apiMockServer = setupServer(
  http.get("/api/v1/invitations", () =>
    HttpResponse.json({
      content: [
        {
          email: "guest+supper@example.com",
          id: "invitation-supper-club",
          invitedAt: "2026-07-17T12:00:00Z",
          invitedByName: "Sam Ortega",
          polityId: "sunday-supper-club",
          polityName: "Sunday Supper Club",
          status: "pending",
        },
        {
          email: "guest+garden@example.com",
          id: "invitation-garden-cooperative",
          invitedAt: "2026-07-16T12:00:00Z",
          invitedByName: "Mira Chen",
          polityId: "garden-cooperative",
          polityName: "Garden Cooperative",
          status: "pending",
        },
        {
          email: "guest+books@example.com",
          id: "invitation-book-circle",
          invitedAt: "2026-07-14T12:00:00Z",
          invitedByName: "Alex Rivera",
          polityId: "local-book-circle",
          polityName: "Local Book Circle",
          status: "pending",
        },
        {
          email: "guest+cabin@example.com",
          id: "invitation-cabin-council",
          invitedAt: "2026-07-12T12:00:00Z",
          invitedByName: "Jon Bell",
          polityId: "cabin-council",
          polityName: "Cabin Council",
          status: "pending",
        },
      ],
      page: { number: 0, size: 100, totalElements: 4, totalPages: 1 },
    }),
  ),
  http.post("/api/v1/invitations/:invitationId/accept", () =>
    HttpResponse.json(
      { id: "accepted-membership", status: "active" },
      { status: 201 },
    ),
  ),
  http.get("/api/v1/invitation-tokens/:token", () =>
    HttpResponse.json({
      expiresAt: "2026-07-20T10:00:00Z",
      invitedEmail: "friend@example.com",
      polityId: "sunday-supper-club",
      polityName: "Sunday Supper Club",
    }),
  ),
);
