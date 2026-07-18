import { HttpResponse, http, type RequestHandler } from "msw";

type CompletionStatus =
  | "requested"
  | "awaiting_identity"
  | "completed"
  | "failed";

type CompletionState = {
  attemptCount: number;
  pollCount: number;
  status?: CompletionStatus;
};

type InvitationResponse = Readonly<{
  email: string;
  id: string;
  invitedAt: string;
  invitedByName: string;
  polityId: string;
  polityName: string;
  status: "pending";
}>;

const completionCreatedAt = "2026-07-18T10:00:00Z";
const completionUpdatedAt = "2026-07-18T10:01:00Z";

const invitationResponses: readonly InvitationResponse[] = [
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
];

const invitationTokenContexts = {
  "invitation-completed": {
    expiresAt: "2026-07-20T10:00:00Z",
    invitedEmail: "completed@example.com",
    polityId: "sunday-supper-club",
    polityName: "Sunday Supper Club",
  },
  "invitation-failed": {
    expiresAt: "2026-07-20T10:00:00Z",
    invitedEmail: "retry@example.com",
    polityId: "sunday-supper-club",
    polityName: "Sunday Supper Club",
  },
  "invitation-pending": {
    expiresAt: "2026-07-20T10:00:00Z",
    invitedEmail: "pending@example.com",
    polityId: "sunday-supper-club",
    polityName: "Sunday Supper Club",
  },
  "invitation-supper-club": {
    expiresAt: "2026-07-20T10:00:00Z",
    invitedEmail: "friend@example.com",
    polityId: "sunday-supper-club",
    polityName: "Sunday Supper Club",
  },
} as const;

type InvitationToken = keyof typeof invitationTokenContexts;

function isInvitationToken(token: string): token is InvitationToken {
  return token in invitationTokenContexts;
}

function completionResponse(state: CompletionState, status: CompletionStatus) {
  return {
    actionExpiresAt:
      status === "awaiting_identity" ? "2026-07-18T10:16:00Z" : undefined,
    attemptCount: state.attemptCount,
    completedAt: status === "completed" ? "2026-07-18T10:01:00Z" : undefined,
    createdAt: completionCreatedAt,
    lastError: status === "failed" ? "credential_action_expired" : undefined,
    status,
    updatedAt: completionUpdatedAt,
  };
}

export function createMembershipInvitationScenarioHandlers(): RequestHandler[] {
  let invitations = [...invitationResponses];
  const completions = new Map<InvitationToken, CompletionState>();

  return [
    http.get("/api/v1/invitations", () =>
      HttpResponse.json({
        content: invitations,
        page: {
          number: 0,
          size: 100,
          totalElements: invitations.length,
          totalPages: 1,
        },
      }),
    ),
    http.post("/api/v1/invitations/:invitationId/accept", ({ params }) => {
      const invitationId = String(params.invitationId);
      const invitation = invitations.find(
        (candidate) => candidate.id === invitationId,
      );

      if (!invitation) {
        return HttpResponse.json(
          { code: "invitation_not_found", message: "Invitation not found." },
          { status: 404 },
        );
      }

      invitations = invitations.filter(
        (candidate) => candidate.id !== invitationId,
      );

      return HttpResponse.json(
        { id: `membership-${invitation.polityId}`, status: "active" },
        { status: 201 },
      );
    }),
    http.get("/api/v1/invitation-tokens/:token", ({ params }) => {
      const token = String(params.token);

      return isInvitationToken(token)
        ? HttpResponse.json(invitationTokenContexts[token])
        : HttpResponse.json(
            { code: "invitation_not_found", message: "Invitation not found." },
            { status: 404 },
          );
    }),
    http.post("/api/v1/invitation-tokens/:token/completion", ({ params }) => {
      const token = String(params.token);
      if (!isInvitationToken(token)) {
        return HttpResponse.json(
          {
            code: "invitation_not_found",
            message: "Invitation not found.",
          },
          { status: 404 },
        );
      }

      const previous = completions.get(token);
      const state: CompletionState = {
        attemptCount: (previous?.attemptCount ?? 0) + 1,
        pollCount: 0,
      };
      const status =
        token === "invitation-completed"
          ? "completed"
          : token === "invitation-failed" && !previous
            ? "failed"
            : "requested";
      state.status = status;
      completions.set(token, state);

      return HttpResponse.json(completionResponse(state, status), {
        status: 202,
      });
    }),
    http.get("/api/v1/invitation-tokens/:token/completion", ({ params }) => {
      const token = String(params.token);
      if (!isInvitationToken(token)) {
        return HttpResponse.json(
          {
            code: "invitation_not_found",
            message: "Invitation not found.",
          },
          { status: 404 },
        );
      }

      const state = completions.get(token);
      if (!state?.status) {
        return HttpResponse.json(
          {
            code: "completion_not_requested",
            message: "Completion has not been requested.",
          },
          { status: 404 },
        );
      }

      if (state.status === "requested") {
        state.pollCount += 1;
        state.status =
          state.pollCount === 1 ? "awaiting_identity" : "completed";
      } else if (state.status === "awaiting_identity") {
        state.pollCount += 1;
        state.status = "completed";
      }

      return HttpResponse.json(completionResponse(state, state.status));
    }),
  ];
}
