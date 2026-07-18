import { describe, expect, it } from "vitest";

import {
  createMembershipInvitationScenarioHandlers,
  membershipInvitationScenario,
} from "@/mocks/scenarios/membership-invitations";
import { apiMockServer } from "@/test/mocks/server";

type CompletionResponse = Readonly<{
  attemptCount: number;
  lastError?: string;
  status: string;
}>;

type InvitationPageResponse = Readonly<{
  content: readonly Readonly<{ id: string; invitedAt: string }>[];
}>;

type InvitationTokenResponse = Readonly<{
  expiresAt: string;
}>;

async function responseJson<T>(response: Response): Promise<T> {
  return response.json() as Promise<T>;
}

async function completionRequest(token: string) {
  return fetch(`/api/v1/invitation-tokens/${token}/completion`, {
    method: "POST",
  });
}

async function completionPoll(token: string) {
  return fetch(`/api/v1/invitation-tokens/${token}/completion`);
}

describe("membership invitation development scenario", () => {
  it("keeps user-visible dates current when the scenario is recreated", async () => {
    apiMockServer.use(
      ...createMembershipInvitationScenarioHandlers({
        now: new Date("2030-04-10T12:00:00Z"),
      }),
    );

    const invitations = await fetch("/api/v1/invitations").then((response) =>
      responseJson<InvitationPageResponse>(response),
    );
    const token = await fetch(
      `/api/v1/invitation-tokens/${membershipInvitationScenario.tokens.pending}`,
    ).then((response) => responseJson<InvitationTokenResponse>(response));

    expect(invitations.content[0]?.invitedAt).toBe("2030-04-09T12:00:00.000Z");
    expect(token.expiresAt).toBe("2030-04-17T12:00:00.000Z");
  });

  it("supports listing and acceptance with isolated in-memory state", async () => {
    apiMockServer.use(...createMembershipInvitationScenarioHandlers());

    const initial = await fetch("/api/v1/invitations").then((response) =>
      responseJson<InvitationPageResponse>(response),
    );
    expect(initial.content).toHaveLength(4);

    const accepted = await fetch(
      `/api/v1/invitations/${membershipInvitationScenario.invitationIds.supperClub}/accept`,
      { method: "POST" },
    );
    expect(accepted.status).toBe(201);

    const remaining = await fetch("/api/v1/invitations").then((response) =>
      responseJson<InvitationPageResponse>(response),
    );
    expect(remaining.content).toHaveLength(3);
    expect(
      remaining.content.some(
        (invitation) =>
          invitation.id ===
          membershipInvitationScenario.invitationIds.supperClub,
      ),
    ).toBe(false);
  });

  it("moves a pending completion through polling to completion", async () => {
    apiMockServer.use(...createMembershipInvitationScenarioHandlers());

    const requested = await completionRequest(
      membershipInvitationScenario.tokens.pending,
    ).then((response) => responseJson<CompletionResponse>(response));
    const awaiting = await completionPoll(
      membershipInvitationScenario.tokens.pending,
    ).then((response) => responseJson<CompletionResponse>(response));
    const completed = await completionPoll(
      membershipInvitationScenario.tokens.pending,
    ).then((response) => responseJson<CompletionResponse>(response));

    expect([requested.status, awaiting.status, completed.status]).toEqual([
      "requested",
      "awaiting_identity",
      "completed",
    ]);
  });

  it("supports a terminal failure followed by a successful retry", async () => {
    apiMockServer.use(...createMembershipInvitationScenarioHandlers());

    const failed = await completionRequest(
      membershipInvitationScenario.tokens.failed,
    ).then((response) => responseJson<CompletionResponse>(response));
    const retried = await completionRequest(
      membershipInvitationScenario.tokens.failed,
    ).then((response) => responseJson<CompletionResponse>(response));

    expect(failed).toMatchObject({
      lastError: "credential_action_expired",
      status: "failed",
    });
    expect(retried).toMatchObject({ attemptCount: 2, status: "requested" });
  });
});
