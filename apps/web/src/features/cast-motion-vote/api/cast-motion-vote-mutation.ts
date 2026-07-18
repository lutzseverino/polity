import {
  mutationOptions,
  useMutation,
  useQueryClient,
} from "@tanstack/react-query";
import type { Motion } from "@/domains/motion";
import {
  type MotionResponse,
  type Polity,
  polityMotionQueryOptions,
  polityQueryOptions,
  reconcileMotionResponse,
} from "@/domains/polity";

import {
  type CastMotionVoteInput,
  castMotionVote,
} from "@/features/cast-motion-vote/api/cast-motion-vote-request";

function castMotionVoteMutationOptions(locale: string) {
  return mutationOptions<MotionResponse, Error, CastMotionVoteInput>({
    mutationFn: (input) =>
      castMotionVote({ ...input, acceptedLanguage: locale }),
    mutationKey: ["motions", "cast-motion-vote"],
  });
}

export function useCastMotionVote(locale: string) {
  const queryClient = useQueryClient();
  return useMutation({
    ...castMotionVoteMutationOptions(locale),
    onSuccess: (response, { motionId, polityId }) => {
      const input = { locale, motionId, polityId };
      queryClient.setQueryData<Motion>(
        polityMotionQueryOptions(input).queryKey,
        (motion) =>
          motion ? reconcileMotionResponse(motion, response, locale) : motion,
      );
      queryClient.setQueryData<Polity>(
        polityQueryOptions({ locale, polityId }).queryKey,
        (polity) =>
          polity
            ? {
                ...polity,
                attention: response.currentVote
                  ? polity.attention.filter(
                      ({ target }) =>
                        target.kind !== "motion" ||
                        target.motionId !== motionId,
                    )
                  : polity.attention,
                motions: polity.motions.map((motion) =>
                  motion.id === motionId
                    ? reconcileMotionResponse(motion, response, locale)
                    : motion,
                ),
              }
            : polity,
      );
      void queryClient.invalidateQueries({
        queryKey: ["polities", "detail", polityId],
      });
      void queryClient.invalidateQueries({ queryKey: ["inbox"] });
    },
  });
}
