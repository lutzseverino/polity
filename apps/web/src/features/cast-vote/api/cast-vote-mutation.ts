import { mutationOptions, useMutation } from "@tanstack/react-query";

import {
  type CastVoteInput,
  castVote,
} from "@/features/cast-vote/api/cast-vote-request";

function castVoteMutationOptions() {
  return mutationOptions<CastVoteInput, Error, CastVoteInput>({
    mutationFn: castVote,
    mutationKey: ["motions", "cast-vote"],
  });
}

export function useCastVote() {
  return useMutation(castVoteMutationOptions());
}
