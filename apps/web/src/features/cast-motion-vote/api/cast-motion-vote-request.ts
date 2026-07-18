import { createHttpClient } from "@/api/http-client";
import type { VoteChoice } from "@/domains/motion";
import { parseMotionResponse } from "@/domains/polity";

const httpClient = createHttpClient();

export type CastMotionVoteInput = Readonly<{
  choice: VoteChoice;
  motionId: string;
  polityId: string;
}>;

export async function castMotionVote(
  input: CastMotionVoteInput & Readonly<{ acceptedLanguage: string }>,
) {
  parseMotionResponse(
    await httpClient.request<unknown, { choice: VoteChoice }>({
      acceptedLanguage: input.acceptedLanguage,
      data: { choice: input.choice },
      method: "PUT",
      url: `/polities/${encodeURIComponent(input.polityId)}/motions/${encodeURIComponent(input.motionId)}/votes`,
    }),
  );
  return input;
}
