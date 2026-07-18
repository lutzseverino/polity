import { describe, expect, it } from "vitest";

import { createMembershipInvitationScenarioHandlers } from "@/mocks/scenarios/membership-invitations";
import { apiMockServer } from "@/test/mocks/server";

type CompletionResponse = Readonly<{
  attemptCount: number;
  lastError?: string;
  status: string;
}>;

type InvitationPageResponse = Readonly<{
  content: readonly Readonly<{ id: string }>[];
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
  it("supports listing and acceptance with isolated in-memory state", async () => {
    apiMockServer.use(...createMembershipInvitationScenarioHandlers());

    const initial = await fetch("/api/v1/invitations").then((response) =>
      responseJson<InvitationPageResponse>(response),
    );
    expect(initial.content).toHaveLength(4);

    const accepted = await fetch(
      "/api/v1/invitations/invitation-supper-club/accept",
      { method: "POST" },
    );
    expect(accepted.status).toBe(201);

    const remaining = await fetch("/api/v1/invitations").then((response) =>
      responseJson<InvitationPageResponse>(response),
    );
    expect(remaining.content).toHaveLength(3);
    expect(
      remaining.content.some(
        (invitation) => invitation.id === "invitation-supper-club",
      ),
    ).toBe(false);
  });

  it("moves a pending completion through polling to completion", async () => {
    apiMockServer.use(...createMembershipInvitationScenarioHandlers());

    const requested = await completionRequest("invitation-pending").then(
      (response) => responseJson<CompletionResponse>(response),
    );
    const awaiting = await completionPoll("invitation-pending").then(
      (response) => responseJson<CompletionResponse>(response),
    );
    const completed = await completionPoll("invitation-pending").then(
      (response) => responseJson<CompletionResponse>(response),
    );

    expect([requested.status, awaiting.status, completed.status]).toEqual([
      "requested",
      "awaiting_identity",
      "completed",
    ]);
  });

  it("supports a terminal failure followed by a successful retry", async () => {
    apiMockServer.use(...createMembershipInvitationScenarioHandlers());

    const failed = await completionRequest("invitation-failed").then(
      (response) => responseJson<CompletionResponse>(response),
    );
    const retried = await completionRequest("invitation-failed").then(
      (response) => responseJson<CompletionResponse>(response),
    );

    expect(failed).toMatchObject({
      lastError: "credential_action_expired",
      status: "failed",
    });
    expect(retried).toMatchObject({ attemptCount: 2, status: "requested" });
  });
});
