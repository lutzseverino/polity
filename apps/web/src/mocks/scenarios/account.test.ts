import { describe, expect, it } from "vitest";

import { createAccountScenarioHandlers } from "@/mocks/scenarios/account";
import { apiMockServer } from "@/test/mocks/server";

type AccountResponse = Readonly<{
  grants: Readonly<{ status: string }>;
}>;

async function accountResponse(response: Response) {
  return response.json() as Promise<AccountResponse>;
}

function readAccount() {
  return fetch("/api/v1/polity/account");
}

function provisionAccount() {
  return fetch("/api/v1/polity/account", { method: "POST" });
}

describe("account mock scenario", () => {
  it("keeps a provisioned account across subsequent reads", async () => {
    apiMockServer.use(...createAccountScenarioHandlers());

    expect((await readAccount()).status).toBe(404);
    const created = await provisionAccount().then(accountResponse);
    const existing = await readAccount().then(accountResponse);

    expect(created).toEqual(existing);
    expect(existing.grants.status).toBe("applied");
  });

  it("moves a pending receipt to applied after bounded reads", async () => {
    apiMockServer.use(
      ...createAccountScenarioHandlers({
        mode: "pending-to-applied",
        pendingReadsBeforeApplied: 1,
      }),
    );

    expect((await provisionAccount().then(accountResponse)).grants.status).toBe(
      "pending",
    );
    expect((await readAccount().then(accountResponse)).grants.status).toBe(
      "pending",
    );
    expect((await readAccount().then(accountResponse)).grants.status).toBe(
      "applied",
    );
  });

  it("keeps a failed receipt terminal", async () => {
    apiMockServer.use(...createAccountScenarioHandlers({ mode: "failed" }));

    const failed = await provisionAccount().then(accountResponse);
    expect(failed.grants.status).toBe("failed");
    expect((await readAccount().then(accountResponse)).grants.status).toBe(
      "failed",
    );
  });
});
