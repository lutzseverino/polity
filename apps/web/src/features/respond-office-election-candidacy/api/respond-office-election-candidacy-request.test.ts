import { describe, expect, it } from "vitest";

import { getPolityMotion, listPolityMotionResponses } from "@/domains/polity";
import { respondOfficeElectionCandidacy } from "@/features/respond-office-election-candidacy/api/respond-office-election-candidacy-request";

describe("respond to office-election candidacy request", () => {
  it("records candidacy consent through HTTP and removes the follow-up action", async () => {
    const response = await respondOfficeElectionCandidacy({
      acceptedLanguage: "en",
      motionId: "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb2",
      polityId: "11111111-1111-4111-8111-111111111111",
      response: "accepted",
    });
    expect(response.actions.respondCandidacy.available).toBe(false);

    const motions = await listPolityMotionResponses(
      "11111111-1111-4111-8111-111111111111",
      {
        acceptedLanguage: "en",
      },
    );
    const election = motions.find(
      ({ id }) => id === "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb2",
    );
    expect(election?.actions.respondCandidacy.available).toBe(false);
    const projectedElection = await getPolityMotion(
      "11111111-1111-4111-8111-111111111111",
      "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb2",
      { acceptedLanguage: "en" },
    );
    expect(projectedElection.actionAvailability).toMatchObject({
      available: false,
      reasonMessage: "You accepted the nomination.",
    });
  });
});
