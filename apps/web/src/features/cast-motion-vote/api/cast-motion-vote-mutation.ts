import { mutationOptions, useMutation } from "@tanstack/react-query";

import {
  type CastMotionVoteInput,
  castMotionVote,
} from "@/features/cast-motion-vote/api/cast-motion-vote-request";

function castMotionVoteMutationOptions() {
  return mutationOptions<CastMotionVoteInput, Error, CastMotionVoteInput>({
    mutationFn: castMotionVote,
    mutationKey: ["motions", "cast-motion-vote"],
  });
}

export function useCastMotionVote() {
  return useMutation(castMotionVoteMutationOptions());
}
