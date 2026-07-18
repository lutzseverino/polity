import { createHttpClient } from "@/api/http-client";
import { parseMotionResponse } from "@/domains/polity";

const httpClient = createHttpClient();

export type OfficeElectionCandidacyResponse = "accepted" | "declined";

export type RespondOfficeElectionCandidacyInput = Readonly<{
  motionId: string;
  polityId: string;
  response: OfficeElectionCandidacyResponse;
}>;

export async function respondOfficeElectionCandidacy(
  input: RespondOfficeElectionCandidacyInput &
    Readonly<{ acceptedLanguage: string }>,
) {
  return parseMotionResponse(
    await httpClient.request<unknown, { accepted: boolean }>({
      acceptedLanguage: input.acceptedLanguage,
      data: { accepted: input.response === "accepted" },
      method: "PUT",
      url: `/polities/${encodeURIComponent(input.polityId)}/motions/${encodeURIComponent(input.motionId)}/candidacy`,
    }),
  );
}
