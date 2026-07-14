import type { VoteChoice } from "@/domains/motion";

export type CastVoteInput = Readonly<{
  choice: VoteChoice;
  motionId: string;
  polityId: string;
}>;

export function castVote(input: CastVoteInput) {
  return Promise.resolve(input);
}
