import { afterEach, describe, expect, it } from "vitest";

import { getCurrentSession } from "@/domains/session";
import { signOut } from "@/features/sign-out/api/sign-out-request";
import { createSessionScenarioHandlers } from "@/mocks/scenarios/session";
import { setTestCookie } from "@/test/cookies";
import { apiMockServer } from "@/test/mocks/server";

afterEach(() => {
  setTestCookie("cardo.csrf=; Max-Age=0; Path=/");
});

describe("sign out request", () => {
  it("ends the current Cardo session", async () => {
    setTestCookie("cardo.csrf=mock-csrf-token; Path=/");
    apiMockServer.use(...createSessionScenarioHandlers());

    await signOut("en");

    await expect(
      getCurrentSession({ acceptedLanguage: "en" }),
    ).rejects.toMatchObject({ status: 401 });
  });
});
