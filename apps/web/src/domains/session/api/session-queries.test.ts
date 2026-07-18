import { QueryClient } from "@tanstack/react-query";
import { describe, expect, it } from "vitest";

import {
  clearCurrentSession,
  clearSessionDependentQueries,
  currentSessionQueryKey,
  setCurrentSession,
} from "@/domains/session/api/session-queries";

const session = {
  authenticationMethod: "password",
  principal: {
    email: "member@example.com",
    emailVerified: true,
    id: "user-1",
  },
} as const;

describe("session cache", () => {
  it("clears only session-dependent server state", async () => {
    const queryClient = new QueryClient();
    setCurrentSession(queryClient, session);
    await queryClient.fetchQuery({
      meta: { requiresSession: true },
      queryFn: () => Promise.resolve("protected"),
      queryKey: ["protected"],
    });
    await queryClient.fetchQuery({
      queryFn: () => Promise.resolve("public"),
      queryKey: ["public"],
    });

    clearSessionDependentQueries(queryClient);

    expect(queryClient.getQueryData(["protected"])).toBeUndefined();
    expect(queryClient.getQueryData(["public"])).toBe("public");
    expect(queryClient.getQueryData(currentSessionQueryKey)).toEqual(session);

    clearCurrentSession(queryClient);
    expect(queryClient.getQueryData(currentSessionQueryKey)).toBeUndefined();
  });
});
