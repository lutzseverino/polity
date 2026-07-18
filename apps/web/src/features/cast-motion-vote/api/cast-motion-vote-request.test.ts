import { describe, expect, it } from "vitest";

import { listInboxItems } from "@/domains/inbox";
import { getPolityMotion } from "@/domains/polity";
import { castMotionVote } from "@/features/cast-motion-vote/api/cast-motion-vote-request";

describe("cast motion vote request", () => {
  it("records a vote through HTTP and exposes it on the next read", async () => {
    await castMotionVote({
      acceptedLanguage: "en",
      choice: "no",
      motionId: "shared-dinner",
      polityId: "thursday-assembly",
    });

    const motion = await getPolityMotion("thursday-assembly", "shared-dinner", {
      acceptedLanguage: "en",
    });
    expect(motion.currentVote).toBe("no");
    expect(motion.participation?.cast).toBe(6);
    const inboxItems = await listInboxItems({ acceptedLanguage: "en" });
    expect(
      inboxItems.some(
        ({ source }) =>
          source.kind === "motion-vote" && source.motionId === "shared-dinner",
      ),
    ).toBe(false);
  });
});
