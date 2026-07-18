import { describe, expect, it } from "vitest";

import { listInboxItems } from "@/domains/inbox";
import { getPolityMotion } from "@/domains/polity";
import { castMotionVote } from "@/features/cast-motion-vote/api/cast-motion-vote-request";

describe("cast motion vote request", () => {
  it("records a vote through HTTP and exposes it on the next read", async () => {
    const response = await castMotionVote({
      acceptedLanguage: "en",
      choice: "no",
      motionId: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1",
      polityId: "11111111-1111-4111-8111-111111111111",
    });
    expect(response).toMatchObject({
      currentVote: "no",
      id: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1",
    });

    const motion = await getPolityMotion(
      "11111111-1111-4111-8111-111111111111",
      "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1",
      {
        acceptedLanguage: "en",
      },
    );
    expect(motion.currentVote).toBe("no");
    expect(motion.participation?.cast).toBe(6);
    const inboxItems = await listInboxItems({ acceptedLanguage: "en" });
    expect(
      inboxItems.some(
        ({ source }) =>
          source.kind === "motion-vote" &&
          source.motionId === "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1",
      ),
    ).toBe(false);
  });
});
