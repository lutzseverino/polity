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

type InvitationResponseData = Omit<InvitationResponse, "invitedAt">;
type InvitationTokenResponse = Readonly<{
  expiresAt: string;
  invitedEmail: string;
  polityId: string;
  polityName: string;
}>;
type ScenarioOptions = Readonly<{
  now?: Date;
}>;

const invitationResponseData: readonly InvitationResponseData[] = [
  {
    email: "guest+supper@example.com",
    id: "invitation-supper-club",
    invitedByName: "Sam Ortega",
    polityId: "sunday-supper-club",
    polityName: "Sunday Supper Club",
    status: "pending",
  },
  {
    email: "guest+garden@example.com",
    id: "invitation-garden-cooperative",
    invitedByName: "Mira Chen",
    polityId: "garden-cooperative",
    polityName: "Garden Cooperative",
    status: "pending",
  },
  {
    email: "guest+books@example.com",
    id: "invitation-book-circle",
    invitedByName: "Alex Rivera",
    polityId: "local-book-circle",
    polityName: "Local Book Circle",
    status: "pending",
  },
  {
    email: "guest+cabin@example.com",
    id: "invitation-cabin-council",
    invitedByName: "Jon Bell",
    polityId: "cabin-council",
    polityName: "Cabin Council",
    status: "pending",
  },
];

const invitationTokenResponseData = {
  "invitation-completed": {
    invitedEmail: "completed@example.com",
    polityId: "sunday-supper-club",
    polityName: "Sunday Supper Club",
  },
  "invitation-failed": {
    invitedEmail: "retry@example.com",
    polityId: "sunday-supper-club",
    polityName: "Sunday Supper Club",
  },
  "invitation-pending": {
    invitedEmail: "pending@example.com",
    polityId: "sunday-supper-club",
    polityName: "Sunday Supper Club",
  },
  "invitation-supper-club": {
    invitedEmail: "friend@example.com",
    polityId: "sunday-supper-club",
    polityName: "Sunday Supper Club",
  },
} as const;

type InvitationToken = keyof typeof invitationTokenResponseData;

function isInvitationToken(token: string): token is InvitationToken {
  return token in invitationTokenResponseData;
}

function dateTimeAtOffset(now: Date, offsetMs: number) {
  return new Date(now.getTime() + offsetMs).toISOString();
}

function completionResponse(
  state: CompletionState,
  status: CompletionStatus,
  now: Date,
) {
  return {
    actionExpiresAt:
      status === "awaiting_identity"
        ? dateTimeAtOffset(now, 15 * 60 * 1_000)
        : undefined,
    attemptCount: state.attemptCount,
    completedAt:
      status === "completed" ? dateTimeAtOffset(now, 60 * 1_000) : undefined,
    createdAt: now.toISOString(),
    lastError: status === "failed" ? "credential_action_expired" : undefined,
    status,
    updatedAt: dateTimeAtOffset(now, 60 * 1_000),
  };
}

export function createMembershipInvitationScenarioHandlers({
  now = new Date(),
}: ScenarioOptions = {}): RequestHandler[] {
  const dayMs = 24 * 60 * 60 * 1_000;
  let invitations: InvitationResponse[] = invitationResponseData.map(
    (invitation, index) => ({
      ...invitation,
      invitedAt: dateTimeAtOffset(now, -(index + 1) * dayMs),
    }),
  );
  const invitationExpiresAt = dateTimeAtOffset(now, 7 * dayMs);
  const invitationTokenContexts: Record<
    InvitationToken,
    InvitationTokenResponse
  > = {
    "invitation-completed": {
      ...invitationTokenResponseData["invitation-completed"],
      expiresAt: invitationExpiresAt,
    },
    "invitation-failed": {
      ...invitationTokenResponseData["invitation-failed"],
      expiresAt: invitationExpiresAt,
    },
    "invitation-pending": {
      ...invitationTokenResponseData["invitation-pending"],
      expiresAt: invitationExpiresAt,
    },
    "invitation-supper-club": {
      ...invitationTokenResponseData["invitation-supper-club"],
      expiresAt: invitationExpiresAt,
    },
  };
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

      return HttpResponse.json(completionResponse(state, status, now), {
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

      return HttpResponse.json(completionResponse(state, state.status, now));
    }),
  ];
}
