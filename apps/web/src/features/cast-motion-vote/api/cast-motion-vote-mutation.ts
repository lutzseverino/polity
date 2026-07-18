import {
  mutationOptions,
  useMutation,
  useQueryClient,
} from "@tanstack/react-query";

import {
  type CastMotionVoteInput,
  castMotionVote,
} from "@/features/cast-motion-vote/api/cast-motion-vote-request";

function castMotionVoteMutationOptions(locale: string) {
  return mutationOptions<CastMotionVoteInput, Error, CastMotionVoteInput>({
    mutationFn: (input) =>
      castMotionVote({ ...input, acceptedLanguage: locale }),
    mutationKey: ["motions", "cast-motion-vote"],
  });
}

export function useCastMotionVote(locale: string) {
  const queryClient = useQueryClient();
  return useMutation({
    ...castMotionVoteMutationOptions(locale),
    onSuccess: (_, { polityId }) => {
      void queryClient.invalidateQueries({
        queryKey: ["polities", "detail", polityId],
      });
      void queryClient.invalidateQueries({ queryKey: ["inbox"] });
    },
  });
}
