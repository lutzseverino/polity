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

export const membershipInvitationScenario = {
  invitationIds: {
    bookCircle: "91111111-1111-4111-8111-111111111113",
    cabinCouncil: "91111111-1111-4111-8111-111111111114",
    gardenCooperative: "91111111-1111-4111-8111-111111111112",
    supperClub: "91111111-1111-4111-8111-111111111111",
  },
  tokens: {
    completed: "mti_2pW7fN9vL4qR8xKc",
    failed: "mti_6tY3jH8sD1mB5zQa",
    pending: "mti_9kC4rV7nP2xF6wLs",
    supperClub: "mti_4gM8qT1bN7sJ3yZd",
  },
} as const;

const supperClubPolityId = "51111111-1111-4111-8111-111111111111";

const invitationResponseData: readonly InvitationResponseData[] = [
  {
    email: "guest+supper@example.com",
    id: membershipInvitationScenario.invitationIds.supperClub,
    invitedByName: "Sam Ortega",
    polityId: supperClubPolityId,
    polityName: "Sunday Supper Club",
    status: "pending",
  },
  {
    email: "guest+garden@example.com",
    id: membershipInvitationScenario.invitationIds.gardenCooperative,
    invitedByName: "Mira Chen",
    polityId: "52222222-2222-4222-8222-222222222222",
    polityName: "Garden Cooperative",
    status: "pending",
  },
  {
    email: "guest+books@example.com",
    id: membershipInvitationScenario.invitationIds.bookCircle,
    invitedByName: "Alex Rivera",
    polityId: "53333333-3333-4333-8333-333333333333",
    polityName: "Local Book Circle",
    status: "pending",
  },
  {
    email: "guest+cabin@example.com",
    id: membershipInvitationScenario.invitationIds.cabinCouncil,
    invitedByName: "Jon Bell",
    polityId: "54444444-4444-4444-8444-444444444444",
    polityName: "Cabin Council",
    status: "pending",
  },
];

const invitationTokenResponseData = {
  [membershipInvitationScenario.tokens.completed]: {
    invitedEmail: "completed@example.com",
    polityId: supperClubPolityId,
    polityName: "Sunday Supper Club",
  },
  [membershipInvitationScenario.tokens.failed]: {
    invitedEmail: "retry@example.com",
    polityId: supperClubPolityId,
    polityName: "Sunday Supper Club",
  },
  [membershipInvitationScenario.tokens.pending]: {
    invitedEmail: "pending@example.com",
    polityId: supperClubPolityId,
    polityName: "Sunday Supper Club",
  },
  [membershipInvitationScenario.tokens.supperClub]: {
    invitedEmail: "friend@example.com",
    polityId: supperClubPolityId,
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
    [membershipInvitationScenario.tokens.completed]: {
      ...invitationTokenResponseData[
        membershipInvitationScenario.tokens.completed
      ],
      expiresAt: invitationExpiresAt,
    },
    [membershipInvitationScenario.tokens.failed]: {
      ...invitationTokenResponseData[
        membershipInvitationScenario.tokens.failed
      ],
      expiresAt: invitationExpiresAt,
    },
    [membershipInvitationScenario.tokens.pending]: {
      ...invitationTokenResponseData[
        membershipInvitationScenario.tokens.pending
      ],
      expiresAt: invitationExpiresAt,
    },
    [membershipInvitationScenario.tokens.supperClub]: {
      ...invitationTokenResponseData[
        membershipInvitationScenario.tokens.supperClub
      ],
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
        {
          admittedAt: now.toISOString(),
          email: invitation.email,
          id: "58888888-8888-4888-8888-888888888888",
          name: "Invited member",
          status: "active",
          userId: "59999999-9999-4999-8999-999999999999",
        },
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
        token === membershipInvitationScenario.tokens.completed
          ? "completed"
          : token === membershipInvitationScenario.tokens.failed && !previous
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
