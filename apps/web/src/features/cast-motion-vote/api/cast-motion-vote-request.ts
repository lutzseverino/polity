import type { VoteChoice } from "@/domains/motion";

export type CastMotionVoteInput = Readonly<{
  choice: VoteChoice;
  motionId: string;
  polityId: string;
}>;

export function castMotionVote(input: CastMotionVoteInput) {
  return Promise.resolve(input);
}
