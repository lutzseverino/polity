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
  type RespondOfficeElectionCandidacyInput,
  respondOfficeElectionCandidacy,
} from "@/features/respond-office-election-candidacy/api/respond-office-election-candidacy-request";

function respondOfficeElectionCandidacyMutationOptions(locale: string) {
  return mutationOptions<
    MotionResponse,
    Error,
    RespondOfficeElectionCandidacyInput
  >({
    mutationFn: (input) =>
      respondOfficeElectionCandidacy({ ...input, acceptedLanguage: locale }),
    mutationKey: ["motions", "respond-office-election-candidacy"],
  });
}

export function useRespondOfficeElectionCandidacy(locale: string) {
  const queryClient = useQueryClient();
  return useMutation({
    ...respondOfficeElectionCandidacyMutationOptions(locale),
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
                attention: response.actions.respondCandidacy.available
                  ? polity.attention
                  : polity.attention.filter(
                      ({ target }) =>
                        target.kind !== "motion" ||
                        target.motionId !== motionId,
                    ),
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
