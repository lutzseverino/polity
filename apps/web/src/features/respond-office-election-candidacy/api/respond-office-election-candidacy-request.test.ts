import { describe, expect, it } from "vitest";

import { getPolityMotion, listPolityMotionResponses } from "@/domains/polity";
import { respondOfficeElectionCandidacy } from "@/features/respond-office-election-candidacy/api/respond-office-election-candidacy-request";

describe("respond to office-election candidacy request", () => {
  it("records candidacy consent through HTTP and removes the follow-up action", async () => {
    await respondOfficeElectionCandidacy({
      acceptedLanguage: "en",
      motionId: "tribune-election",
      polityId: "thursday-assembly",
      response: "accepted",
    });

    const motions = await listPolityMotionResponses("thursday-assembly", {
      acceptedLanguage: "en",
    });
    const election = motions.find(({ id }) => id === "tribune-election");
    expect(election?.actions.respondCandidacy.available).toBe(false);
    const projectedElection = await getPolityMotion(
      "thursday-assembly",
      "tribune-election",
      { acceptedLanguage: "en" },
    );
    expect(projectedElection.actionAvailability).toMatchObject({
      available: false,
      reasonMessage: "You accepted the nomination.",
    });
  });
});
